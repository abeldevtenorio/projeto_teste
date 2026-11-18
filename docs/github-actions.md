# GitHub Actions: teams do GitHub como roles

Guia de uso da action [tspascoal/get-user-teams-membership](https://github.com/tspascoal/get-user-teams-membership) para transformar **teams de uma organização GitHub** nas **roles** usadas por este projeto (gate de CI/CD e filtro Java).

## Visão geral

```
github.actor ──► get-user-teams-membership ──► ["admins", "developers"]
                                                      │
                                    ┌─────────────────┴─────────────────┐
                                    ▼                                   ▼
                        gate de workflow (jobs)               roles para o filtro Java
                        contains(...) / allowed               ./mvnw ... Main ANY admin
```

- A action devolve **teams**, não roles de aplicação. O mapeamento team → role é explícito.
- Com `use_team_slug: true` (recomendado) os valores são **slugs** (minúsculas, `-` em vez de espaços), seguros para comparar e usar em URLs.
- O gate deste repositório é *fail-closed*: sem token, sem teams ou sem roles exigidas, o acesso é negado.

### Mapeamento usado nos exemplos

| Team (slug) | Role no `users.yml` | Uso típico |
|---|---|---|
| `admins` | `admin` | release, deploy, alterações a configuração sensível |
| `developers` | `developer` | build/testes, previews |
| `qa` | `qa` | testes de integração, aprovação de qualidade |

Se o slug da team for igual à role (`qa` → `qa`), não é preciso mapear. Caso contrário, use um passo `jq` como no [Cenário 3](#cenário-3--teams-do-ator-como-roles-no-filtro-java).

## Autenticação

`GITHUB_TOKEN` nativo **não serve** (não tem permissões de organização). Use uma destas opções e guarde o valor no secret `ORG_READ_TOKEN`:

### 1. PAT classic (mais simples)

1. GitHub → **Settings → Developer settings → Personal access tokens → Tokens (classic)**.
2. Scope: `read:org`.
3. Se a organização usar SSO, aprove o token na organização.
4. No repositório: **Settings → Secrets and variables → Actions → New repository secret** com o nome `ORG_READ_TOKEN`.

### 2. PAT fine-grained

1. **Fine-grained tokens → Generate new token**.
2. **Organization permissions → Members → Read-only**.
3. Selecione a organização e (opcional) só os repositórios necessários.
4. Guarde como `ORG_READ_TOKEN` (pode exigir aprovação de um owner da org).

### 3. GitHub App installation token (recomendado para organizações)

A App precisa de **Organization permissions → Members → Read-only** e deve estar instalada na organização. Gere o token no próprio workflow com [`actions/create-github-app-token`](https://github.com/actions/create-github-app-token):

```yaml
- uses: actions/create-github-app-token@v1
  id: app-token
  with:
    app-id: ${{ vars.APP_ID }}
    private-key: ${{ secrets.PRIVATE_KEY }}
    owner: ${{ github.repository_owner }}

- uses: tspascoal/get-user-teams-membership@v4
  id: membership
  with:
    username: ${{ github.actor }}
    team: 'admins'
    use_team_slug: true
    GITHUB_TOKEN: ${{ steps.app-token.outputs.token }}
```

### Notas de versão

- Use `@v4`: corre em Node.js 24 e evita o aviso de deprecação do Node 20 do `@v3`.
- GitHub Enterprise Server: só é suportado a partir do GHES 3.4 (para versões anteriores, use `@v1`).

## Referência rápida da action

| Input | Obrigatório | Descrição |
|---|---|---|
| `username` | sim | Utilizador cujas teams queremos obter |
| `organization` | não | Default: `${{ github.repository_owner }}` |
| `team` | não | Team(s) a verificar, CSV. A vírgula é **separador**, não faz parte do nome |
| `use_team_slug` | não | `true` para usar slugs em vez de nomes de exibição (default `false`) |
| `GITHUB_TOKEN` | sim | Token com `read:org` ou Members read-only |

| Output | Tipo | Notas |
|---|---|---|
| `teams` | JSON serializado | Use `fromJSON()` para iterar: `contains(fromJSON(steps.x.outputs.teams), 'admins')` |
| `isTeamMember` | string | `'true'`/`'false'`; é sempre `'false'` se `team` não for indicado |

Comparações da action são **case-insensitive**, mas a nossa documentação normaliza para minúsculas para evitar surpresas.

## Workflows deste repositório

| Ficheiro | O que faz |
|---|---|
| `.github/workflows/ci.yml` | Corre `./mvnw -B test` em push/PR (não precisa de token) |
| `.github/workflows/role-gate.yml` | Workflow reutilizável (`workflow_call`) + manual (`workflow_dispatch`) que resolve as teams do ator e expõe `allowed` e `teams` |
| `.github/scripts/role-gate.sh` | Avalia `ANY`/`ALL` entre as teams do ator e as roles exigidas; escreve `allowed` e o resumo do run |

Configuração mínima:

1. Criar o secret `ORG_READ_TOKEN` (ver [Autenticação](#autenticação)).
2. Chamar o gate a partir do workflow que deve ser protegido (exemplos abaixo).

## Cenário 1 — gate de release (fail-closed)

`release.yml`:

```yaml
name: Release

on:
  push:
    tags: ['v*']

permissions:
  contents: read

jobs:
  roles:
    name: Verificar roles do ator
    uses: ./.github/workflows/role-gate.yml
    with:
      teams: 'admins,developers'
      mode: 'ANY'
    secrets:
      ORG_READ_TOKEN: ${{ secrets.ORG_READ_TOKEN }}
    # alternativa: secrets: inherit

  publish:
    name: Publicar release
    needs: roles
    if: needs.roles.outputs.allowed == 'true'
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
      - name: Publicar
        run: echo "A publicar release como ${{ github.actor }}..."
```

- `allowed` é uma **string** (`'true'`/`'false'`): compare com `== 'true'`.
- O job `publish` é **ignorado** quando o ator não tem as roles; o run do gate fica com `warning` e a tabela no *summary*.
- Para exigir todas as roles: `mode: 'ALL'`.

## Cenário 2 — expressões simples (sem workflow reutilizável)

Gate rápido com `isTeamMember`:

```yaml
steps:
  - uses: tspascoal/get-user-teams-membership@v4
    id: membership
    with:
      username: ${{ github.actor }}
      team: 'admins'
      use_team_slug: true
      GITHUB_TOKEN: ${{ secrets.ORG_READ_TOKEN }}

  - name: Bloquear quem não é admin
    if: steps.membership.outputs.isTeamMember == 'false'
    run: |
      echo "::error::${{ github.actor }} não pertence à team admins"
      exit 1
```

Ou várias teams a partir do output `teams`:

```yaml
- name: Verificar perfis
  env:
    TEAMS: ${{ steps.membership.outputs.teams }}
  run: |
    if ! jq -e 'map(ascii_downcase) | any(. == "admins" or . == "developers")' <<< "$TEAMS" > /dev/null; then
      echo "::error::Apenas admins ou developers podem continuar"
      exit 1
    fi
```

## Cenário 3 — teams do ator como roles no filtro Java

Usa as teams do ator como roles para o `UserFilterService` (via `Main`). O `jq` converte slugs em roles:

```yaml
name: Filtro por roles do ator

on:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  demo:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: tspascoal/get-user-teams-membership@v4
        id: membership
        with:
          username: ${{ github.actor }}
          use_team_slug: true
          GITHUB_TOKEN: ${{ secrets.ORG_READ_TOKEN }}

      - name: Mapear teams em roles
        id: roles
        env:
          TEAMS: ${{ steps.membership.outputs.teams }}
        run: |
          roles=$(jq -r 'map({"admins":"admin","developers":"developer","qa":"qa"}[.] // .) | join(",")' <<< "$TEAMS")
          echo "roles=$roles"
          echo "roles=$roles" >> "$GITHUB_OUTPUT"

      - name: Falhar sem roles mapeadas
        if: steps.roles.outputs.roles == ''
        run: |
          echo "::error::${{ github.actor }} não tem teams mapeadas em users.yml"
          exit 1

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Utilizadores com as mesmas roles do ator
        env:
          ROLES: ${{ steps.roles.outputs.roles }}
        run: |
          ./mvnw -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
            -Dexec.mainClass=com.example.users.Main \
            -Dexec.args="ANY $ROLES"
```

Notas:

- `Main` espera `<ANY|ALL> <role> [role...]`; sem roles termina com erro de utilização (fail-closed).
- O `jq` remove teams sem correspondência? Não: `// .` mantém o slug original. Remova o fallback se quiser ignorar teams desconhecidas.
- Em CI, prefira `mode: ALL` quando a operação exige vários perfis (ex.: `admin` **e** `qa`).

## Cenário 4 — GitHub App token

Equivalente ao Cenário 1, mas sem PAT de utilizador (ver [Autenticação](#autenticação)). O segredo necessário passa a ser a chave privada da App:

```yaml
jobs:
  roles:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/create-github-app-token@v1
        id: app-token
        with:
          app-id: ${{ vars.APP_ID }}
          private-key: ${{ secrets.PRIVATE_KEY }}
          owner: ${{ github.repository_owner }}
      - uses: tspascoal/get-user-teams-membership@v4
        id: membership
        with:
          username: ${{ github.actor }}
          team: 'admins,developers'
          use_team_slug: true
          GITHUB_TOKEN: ${{ steps.app-token.outputs.token }}
```

## Segurança

- **Forks**: PRs de forks não recebem secrets. O gate *fail-closed* impede que um contributor externo passe por omissão.
- **`pull_request_target`**: evite combiná-lo com `actions/checkout` do código do PR; é vetor clássico de exfiltração de secrets.
- **Permissões mínimas**: `permissions: contents: read` no topo; eleve para `contents: write` apenas no job protegido.
- **Token**: use PAT fine-grained ou GitHub App em vez de PAT classic com scope amplo; rode tokens periodicamente.
- **Ator**: `github.actor` é quem iniciou o run; num *re-run* considere `github.triggering_actor` para refletir quem o repetiu.
- **Logs**: os nomes das teams não são secretos, mas não imprima tokens; a action já os mascararia apenas se registados como secret.

## Troubleshooting

| Sintoma | Causa provável / solução |
|---|---|
| `Resource not accessible by integration` / 404 | Token sem `read:org`/Members read-only, PAT não aprovado em SSO, ou App não instalada na org |
| `isTeamMember` sempre `'false'` | `team` não foi indicado (comportamento esperado) ou slug errado (use `use_team_slug: true` e o slug real) |
| Team com vírgula no nome não encontrada | A vírgula é separador do input `team`; use o **slug** da team |
| `organization` errada | O default é o owner do repositório; em forks/mirrors defina explicitamente |
| Gate verde com acesso negado | Verifique se o job protegido tem `if: needs.<gate>.outputs.allowed == 'true'` e se está a comparar strings |
| Aviso “Node.js 20 actions are deprecated” | Atualize para `tspascoal/get-user-teams-membership@v4` |
| GHES antigo | Suporte apenas a partir do 3.4 (ou `@v1` para versões anteriores) |
| `MODE inválido` no script | Use `ANY` ou `ALL` (maiúsculas ou minúsculas) |

## Referências

- Action: <https://github.com/tspascoal/get-user-teams-membership>
- Contexto de expressões (`contains`, `fromJSON`): <https://docs.github.com/actions/learn-github-actions/contexts>
- Reusable workflows: <https://docs.github.com/actions/using-workflows/reusing-workflows>
- Scopes OAuth: <https://docs.github.com/apps/building-oauth-apps/understanding-scopes-for-oauth-apps>
- GitHub App tokens: <https://github.com/actions/create-github-app-token>
