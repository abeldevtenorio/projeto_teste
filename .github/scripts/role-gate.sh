#!/usr/bin/env bash
#
# Avalia se as teams do ator (JSON) cobrem as roles exigidas (CSV).
#
# Entradas (env):
#   TEAMS    obrigatorio  JSON array com as teams do ator (output da action)
#   REQUIRED obrigatorio  CSV de roles/teams exigidas
#   MODE     opcional     ANY (default) ou ALL
#
# Saidas:
#   stdout: allowed=true|false
#   $GITHUB_OUTPUT: allowed=...
#   $GITHUB_STEP_SUMMARY: tabela com o resultado
#
# Codigos de saida: 0 = avaliacao feita (permitido ou negado); 2 = configuracao invalida.

set -euo pipefail

teams_json=${TEAMS:-[]}
required_csv=${REQUIRED:-}
mode=$(printf '%s' "${MODE:-ANY}" | tr '[:lower:]' '[:upper:]')

if [ "$mode" != "ANY" ] && [ "$mode" != "ALL" ]; then
  echo "::error::MODE invalido: '$mode' (use ANY ou ALL)" >&2
  exit 2
fi

if ! jq -e 'type == "array"' >/dev/null 2>&1 <<< "$teams_json"; then
  echo "::error::TEAMS tem de ser um array JSON, recebido: $teams_json" >&2
  exit 2
fi

required=()
while IFS= read -r role; do
  [ -n "$role" ] && required+=("$role")
done < <(printf '%s\n' "$required_csv" | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | tr '[:upper:]' '[:lower:]' | sed '/^$/d')

if [ ${#required[@]} -eq 0 ]; then
  echo "::error::Nenhuma role exigida (REQUIRED vazio); a negar por precaucao" >&2
  exit 2
fi

has_role() {
  jq -e --arg role "$1" 'any(.[]; ascii_downcase == $role)' >/dev/null <<< "$teams_json"
}

allowed=false
if [ "$mode" = "ALL" ]; then
  allowed=true
  for role in "${required[@]}"; do
    if ! has_role "$role"; then
      allowed=false
      break
    fi
  done
else
  for role in "${required[@]}"; do
    if has_role "$role"; then
      allowed=true
      break
    fi
  done
fi

echo "allowed=$allowed"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  printf 'allowed=%s\n' "$allowed" >> "$GITHUB_OUTPUT"
fi

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "## Role gate"
    echo
    echo "| Campo | Valor |"
    echo "|---|---|"
    echo "| Ator | \`${GITHUB_ACTOR:-desconhecido}\` |"
    echo "| Teams do ator | \`${teams_json}\` |"
    echo "| Roles exigidas | \`${required_csv}\` |"
    echo "| Modo | \`${mode}\` |"
    echo "| Resultado | **${allowed}** |"
  } >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$allowed" = "true" ]; then
  echo "::notice::Role gate permitido para ${GITHUB_ACTOR:-ator} (${mode}: ${required_csv})"
else
  echo "::warning::Role gate negado para ${GITHUB_ACTOR:-ator} (${mode}: ${required_csv})"
fi
