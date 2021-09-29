#!/bin/bash

set -eu

readonly BWA=${BWA:-bwa}
readonly SAMTOOLS=${SAMTOOLS:-samtools}
readonly FUSIONFUSION=${FUSIONFUSION:-fusionfusion}
readonly DUXHUND_JAR=${DUXHUND_JAR:-/opt/duxhund/duxhund.jar}
readonly BWA_OPTS=${BWA_OPTS:-'-t 8 -T 0'}
readonly SAMTOOLS_OPTS=${SAMTOOLS_OPTS:-'-@ 8'}
readonly FUSIONFUSION_OPTS=${FUSIONFUSION_OPTS:-'--no_blat --grc --genome_id hg38'}
readonly DUXHUND_GENERATE_OPTS=${DUXHUND_GENERATE_OPTS:-'--min-softclip-len 20'}
readonly DUXHUND_FIXUP_OPTS=${DUXHUND_FIXUP_OPTS:-''}

reference=''
masked_reference=''
target=''
r1=''
r2=''
outdir=''

while getopts ":-:" optchr; do
  case "$optchr" in
    -)
      case "$OPTARG" in
        reference)
          reference="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        masked-reference)
          masked_reference="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        target)
          target="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        r1)
          r1="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        r2)
          r2="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        output)
          outdir="${!OPTIND}"
          OPTIND=$((OPTIND + 1))
          ;;
        *)
          echo "Undefined option: ${!OPTIND}"
          exit 1
          ;;
      esac
      ;;
    *)
      ;;
  esac
done

if [ -z $reference ] || [ -z $masked_reference ] || [ -z $target ] || [ -z $r1 ] || [ -z $r2 ] || [ -z $outdir ]; then
  echo "Error: Missing required arguments: --reference, --masked-reference, --target, --r1, --r2 or --output"
  exit 1
fi

mkdir -p "$outdir"

# shellcheck disable=SC2086
"$BWA" mem $BWA_OPTS -a "$reference" "$r1" "$r2" | "$SAMTOOLS" view $SAMTOOLS_OPTS -b > "$outdir/aligned.bam"

# shellcheck disable=SC2086
java -cp "$DUXHUND_JAR" duxhund.cli generate-fastq \
  --input "$outdir/aligned.bam" \
  --target "$target" \
  --output "$outdir" \
  $DUXHUND_GENERATE_OPTS

# shellcheck disable=SC2086
"$BWA" mem $BWA_OPTS -a -p -C "$masked_reference" "$outdir/out.fastq" > "$outdir/realigned.sam"

# shellcheck disable=SC2086
java -cp "$DUXHUND_JAR" duxhund.cli fixup-sam \
  --input "$outdir/realigned.sam" \
  --saved-seqs "$outdir/saved-seqs.edn" \
  --output "$outdir/realigned.fixed.sam" \
  $DUXHUND_FIXUP_OPTS

fusion_opts="$FUSIONFUSION_OPTS"
if [ -n "${DEBUG-}" ]; then
  fusion_opts="$fusion_opts --debug"
fi
# shellcheck disable=SC2086
"$FUSIONFUSION" $fusion_opts \
  --reference_genome "$masked_reference" \
  --star "$outdir/realigned.fixed.sam" \
  --out "$outdir/fusionfusion"

cp "$outdir/fusionfusion/fusion_fusion.result.txt" "$outdir/result.txt"

if [ -z "${DEBUG-}" ]; then
  rm -rf "$outdir/aligned.bam" \
    "$outdir/out.fastq" \
    "$outdir/saved-seqs.edn" \
    "$outdir/realigned.sam" \
    "$outdir/fusionfusion"
fi
