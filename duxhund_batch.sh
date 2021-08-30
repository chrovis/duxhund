#!/bin/bash

set -eu

# shellcheck disable=SC2155
readonly SCRIPT_DIR="$(dirname "$(realpath "$0")")"
readonly MAX_RANDOM_WAIT_SECS=${MAX_RANDOM_WAIT_SECS:-60}

wait_random_time() {
  sleep $((RANDOM % MAX_RANDOM_WAIT_SECS + 1))
}

download_file() {
  local src="$1"
  local dst="$2"
  wait_random_time
  aws s3 cp "$src" "$dst" --only-show-errors
}

# shellcheck disable=SC2155
download_reference() {
  local src="$1"
  local dst="$2"
  local dir="$(dirname "$src")"
  local filename="$(basename "$src")"
  wait_random_time
  aws s3 cp "$dir" "$dst" --recursive --exclude "*" --include "$filename*" --only-show-errors
}

reference=''
masked_reference=''
target=''
r1=''
r2=''
output=''

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
          output="${!OPTIND}"
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

if [ -z $reference ] || [ -z $masked_reference ] || [ -z $target ] || [ -z $r1 ] || [ -z $r2 ] || [ -z $output ]; then
  echo "Error: Missing required arguments: --reference, --masked-reference, --target, --r1, --r2 or --output"
  exit 1
fi

reference_local="reference"
masked_reference_local="masked_reference"
mkdir -p "$reference_local" "$masked_reference_local"
download_reference "$reference" "$reference_local"
download_reference "$masked_reference" "$masked_reference_local"

for file in "$target" "$r1" "$r2"; do
  download_file "$file" .
done

output_local="output"
mkdir -p "$output_local"

"$SCRIPT_DIR/duxhund.sh" \
  --reference "$reference_local/$(basename "$reference")" \
  --masked-reference "$masked_reference_local/$(basename "$masked_reference")" \
  --target "$(basename "$target")" \
  --r1 "$(basename "$r1")" \
  --r2 "$(basename "$r2")" \
  --output "$output_local"

aws s3 cp "$output_local" "$output" --recursive --only-show-errors
