# users-role-filter

Exemplo de **filtragem de utilizadores por roles** em Java (JUnit 5 + Mockito + AssertJ), com as roles definidas num ficheiro de configuração YAML. Inclui integração com **GitHub Actions** para usar teams de uma organização GitHub como roles.

## Estrutura

```
src/main/java/com/example/users/
├── Main.java                    # demo: filtra users.yml e imprime o resultado
├── model/User.java              # record username + roles normalizadas
├── model/RoleMatchMode.java     # ANY (pelo menos uma role) / ALL (todas as roles)
├── config/UsersConfigLoader.java# lê e valida o YAML
├── config/UsersConfigException.java
├── repository/UserRepository.java
├── repository/ConfigUserRepository.java
└── service/UserFilterService.java

src/main/resources/users.yml     # configuração de exemplo
src/test/...                     # testes unitários (Mockito) + testes de configuração

docs/github-actions.md           # guia de uso com GitHub Actions (teams como roles)
.github/workflows/ci.yml         # testes em CI
.github/workflows/role-gate.yml  # workflow reutilizável de gate por roles
.github/scripts/role-gate.sh     # avaliação ANY/ALL sobre as teams do ator
```

## Configuração (`users.yml`)

```yaml
users:
  - username: alice
    roles: [admin, developer]
  - username: bob
    roles: [developer]
  - username: carol
    roles: [qa, developer]
```

As roles são normalizadas ao carregar: `" Admin "` e `ADMIN` passam a `admin`; duplicados e valores vazios são descartados.

## Regras do filtro

| Regra | Comportamento |
|---|---|
| Comparação | Case-insensitive, espaços extra ignorados |
| `RoleMatchMode.ANY` | Utilizador entra se tiver **pelo menos uma** das roles pedidas (modo por omissão) |
| `RoleMatchMode.ALL` | Utilizador entra apenas se tiver **todas** as roles pedidas |
| Filtro sem roles | Lança `IllegalArgumentException` (nunca devolve todos por engano) |
| Ordem do resultado | Ordenado por `username`, ignorando maiúsculas/minúsculas |

## GitHub Actions (teams como roles)

A action [`tspascoal/get-user-teams-membership`](https://github.com/tspascoal/get-user-teams-membership) obtém as teams do utilizador na organização; essas teams são usadas como roles para proteger jobs.

```yaml
- uses: tspascoal/get-user-teams-membership@v4
  id: membership
  with:
    username: ${{ github.actor }}
    team: 'admins,developers'
    use_team_slug: true
    GITHUB_TOKEN: ${{ secrets.ORG_READ_TOKEN }}
```

Gate reutilizável deste repositório (`.github/workflows/role-gate.yml`):

```yaml
jobs:
  roles:
    uses: ./.github/workflows/role-gate.yml
    with:
      teams: 'admins,developers'
      mode: 'ANY'
    secrets:
      ORG_READ_TOKEN: ${{ secrets.ORG_READ_TOKEN }}

  deploy:
    needs: roles
    if: needs.roles.outputs.allowed == 'true'
    runs-on: ubuntu-latest
    steps:
      - run: echo "Apenas membros de admins/developers chegam aqui."
```

Requisitos e cenários completos (PAT classic, fine-grained, GitHub App, segurança, troubleshooting) em **[docs/github-actions.md](docs/github-actions.md)**.

## Correr os testes

```bash
mvn test
```

Apenas uma classe:

```bash
mvn test -Dtest=UserFilterServiceTest
```

Demo com o `users.yml`:

```bash
./mvnw -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=com.example.users.Main -Dexec.args="ANY developer"
```

## Testes incluídos

- `UserFilterServiceTest` - unidade com `UserRepository` **mockado** (Mockito): modo ANY/ALL, case-insensitive, sem duplicados, filtro vazio inválido, repositório chamado uma única vez e propagação de falhas.
- `UsersConfigLoaderTest` - leitura/validação do YAML, incluindo ficheiros inválidos.
- `ConfigUserRepositoryTest` - repositório configurado por classpath/ficheiro + integração com o filtro.
- `UserTest` - normalização das roles e validação do username.
