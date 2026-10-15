# Inventory Control (SimpleStock)

API REST de controle de estoque: categorias, fornecedores, produtos e movimentações (entrada, saída e ajuste), com autenticação JWT e controle de acesso por **roles** e **permissões**.

- Java 25, Spring Boot 4, Spring Security, Spring Data JPA, PostgreSQL
- Documentação interativa (Swagger UI): `http://localhost:8080/swagger-ui.html`

## Como executar

Pré-requisitos: JDK 25 e um PostgreSQL com o banco `simplestock` (ver `src/main/resources/application.properties`).

```bash
export SUPERUSER_PASSWORD='uma-senha-forte'   # obrigatório para criar o Super Usuário (veja abaixo)
./mvnw spring-boot:run
```

### Configuração

| Propriedade | Variável de ambiente | Padrão | Descrição |
|---|---|---|---|
| `app.superuser.username` | `SUPERUSER_USERNAME` | `superuser` | Nome do Super Usuário |
| `app.superuser.email` | `SUPERUSER_EMAIL` | `superuser@simplestock.local` | E-mail (login) do Super Usuário |
| `app.superuser.password` | `SUPERUSER_PASSWORD` | *(vazio)* | Senha do Super Usuário. **Sem padrão de propósito**: se estiver vazia, nenhum Super Usuário é criado e um aviso é logado |
| `secretKey` | — | `secret` | Segredo de assinatura do JWT. Troque em produção |
| `file.upload-dir` | — | `./shared/files/products` | Pasta das imagens dos produtos |

## Autenticação

1. `POST /api/v1/auth/register` — cria uma conta. **Toda conta nova é `USER` e não tem nenhuma permissão** até que alguém as conceda. O corpo só aceita `username`, `email` e `password`; não é possível escolher a role no cadastro. E-mail já cadastrado retorna `409`.
2. `POST /api/v1/auth/login` — retorna `{ "token": "..." }` (JWT válido por 24h).
3. Envie o token nas demais chamadas: `Authorization: Bearer <token>`.

Sem token (ou com token inválido) a resposta é `403`. Com token válido, mas sem a permissão exigida pelo endpoint, também é `403`.

## Roles e permissões

### Roles

| Role | O que é |
|---|---|
| `SUPER_USER` | Conta administrativa. Pode tudo no sistema, incluindo promover/rebaixar admins e delegar o gerenciamento de permissões. A conta **não pode ser alterada pela API** (nem por ela mesma) e a role não pode ser atribuída a ninguém. É criada na inicialização (veja [Super Usuário](#super-usuário)) |
| `ADMIN` | Possui todas as permissões e pode conceder/revogar permissões de usuários comuns |
| `USER` | Possui apenas as permissões concedidas individualmente |

### Permissões

Cada endpoint exige uma permissão (authority do Spring Security com o mesmo nome).

| Recurso | Permissões |
|---|---|
| Categorias | `CATEGORY_READ`, `CATEGORY_CREATE`, `CATEGORY_UPDATE`, `CATEGORY_DELETE` |
| Fornecedores | `SUPPLIER_READ`, `SUPPLIER_CREATE`, `SUPPLIER_UPDATE`, `SUPPLIER_DELETE` |
| Produtos | `PRODUCT_READ`, `PRODUCT_CREATE`, `PRODUCT_UPDATE`, `PRODUCT_DELETE`, `PRODUCT_EXPORT` (planilha) |
| Movimentações | `MOVEMENT_READ`, `MOVEMENT_CREATE` (entrada, saída e add), `MOVEMENT_UPDATE`, `MOVEMENT_DELETE` |
| Usuários | `USER_READ` (listar/consultar), `USER_MANAGE_PERMISSIONS` (conceder/revogar permissões) |

A lista atual também está disponível em `GET /api/v1/users/permissions`.

### Permissões efetivas

`permissões efetivas = permissões padrão da role + permissões concedidas individualmente`

- `SUPER_USER` e `ADMIN`: todas as permissões
- `USER`: somente as concedidas

As permissões são lidas do banco **a cada requisição**. Conceder, revogar ou mudar a role de alguém vale imediatamente, sem precisar de novo login, e o token de um usuário excluído deixa de funcionar.

### Regras de concessão

`PUT /api/v1/users/{id}/permissions` (exige `USER_MANAGE_PERMISSIONS`) **substitui** o conjunto de permissões individuais do usuário. As regras abaixo evitam escalada de privilégio:

- ninguém altera as próprias permissões;
- a conta do Super Usuário não pode ser alterada;
- só o Super Usuário altera as permissões de um `ADMIN`;
- só o Super Usuário concede ou revoga `USER_MANAGE_PERMISSIONS` (a *delegação* do gerenciamento). Assim, um `USER` com essa permissão pode atuar como gerente de permissões;
- quem não é Super Usuário só concede ou revoga permissões que ele próprio possui.

`PUT /api/v1/users/{id}/role` é exclusivo do Super Usuário: define `ADMIN` ou `USER`. Não é possível atribuir `SUPER_USER`, alterar o Super Usuário ou a própria role.

Violações dessas regras retornam `403` com a mensagem do motivo; usuário inexistente retorna `404`; permissão ou role desconhecida no corpo retorna `400`.

### Super Usuário

Na inicialização, `SuperUserInitializer` garante a conta do Super Usuário a partir de `app.superuser.*`:

- se não existe conta com aquele e-mail e há senha configurada, ela é criada;
- se já existe uma conta com aquele e-mail, ela é **promovida** a `SUPER_USER` (a senha não é alterada);
- sem senha configurada, nada é criado.

## Endpoints

Base: `/api/v1`. "Permissão" é a exigida para chamar o endpoint.

### Auth (público)

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastro (`USER` sem permissões) |
| POST | `/auth/login` | Login, retorna o JWT |

### Usuários

| Método | Caminho | Permissão | Descrição |
|---|---|---|---|
| GET | `/users/me` | autenticado | Dados do usuário logado, com role e permissões efetivas |
| GET | `/users/permissions` | autenticado | Lista de todas as permissões existentes |
| GET | `/users` | `USER_READ` | Lista paginada de usuários |
| GET | `/users/{id}` | `USER_READ` | Consulta um usuário |
| PUT | `/users/{id}/permissions` | `USER_MANAGE_PERMISSIONS` | Substitui as permissões individuais |
| PUT | `/users/{id}/role` | role `SUPER_USER` | Define a role (`ADMIN` ou `USER`) |

Exemplos:

```http
PUT /api/v1/users/5/permissions
Authorization: Bearer <token>
Content-Type: application/json

{ "permissions": ["PRODUCT_READ", "MOVEMENT_READ", "MOVEMENT_CREATE"] }
```

```http
PUT /api/v1/users/5/role
{ "role": "ADMIN" }
```

Resposta (`UserResponseDTO`):

```json
{
  "id": 5,
  "username": "maria",
  "email": "maria@mail.com",
  "role": "USER",
  "permissions": ["MOVEMENT_CREATE", "MOVEMENT_READ", "PRODUCT_READ"],
  "effectivePermissions": ["MOVEMENT_CREATE", "MOVEMENT_READ", "PRODUCT_READ"]
}
```

### Categorias

| Método | Caminho | Permissão |
|---|---|---|
| GET | `/categories` | `CATEGORY_READ` |
| GET | `/categories/{name}` | `CATEGORY_READ` |
| POST | `/categories/add` | `CATEGORY_CREATE` |
| PUT | `/categories/update/{id}` | `CATEGORY_UPDATE` |
| DELETE | `/categories/delete/{id}` | `CATEGORY_DELETE` |

`GET /categories` e `GET /categories/{name}` devolvem também `productCount`, o número de produtos vinculados à categoria (`0` se não houver). Na listagem ele vem de uma única consulta agrupada por página, sem carregar os produtos. Quando a categoria aparece dentro de um produto o campo é omitido.

### Fornecedores

| Método | Caminho | Permissão |
|---|---|---|
| GET | `/supplier` | `SUPPLIER_READ` |
| POST | `/supplier/add` | `SUPPLIER_CREATE` |
| PUT | `/supplier/update/{id}` | `SUPPLIER_UPDATE` |
| DELETE | `/supplier/delete/{id}` | `SUPPLIER_DELETE` |

### Produtos

| Método | Caminho | Permissão |
|---|---|---|
| GET | `/product` | `PRODUCT_READ` |
| POST | `/product/add` (multipart: `product` JSON + `file` opcional) | `PRODUCT_CREATE` |
| PUT | `/product/update/{id}` | `PRODUCT_UPDATE` |
| DELETE | `/product/delete/{id}` | `PRODUCT_DELETE` |
| GET | `/product/image/{arquivo}` (miniatura do produto) | `PRODUCT_READ` |
| GET | `/product/download` (planilha `.xls`) | `PRODUCT_EXPORT` |

### Movimentações

| Método | Caminho | Permissão |
|---|---|---|
| GET | `/movement` | `MOVEMENT_READ` |
| POST | `/movement/inbound` | `MOVEMENT_CREATE` |
| POST | `/movement/outbound` | `MOVEMENT_CREATE` |
| POST | `/movement/add` | `MOVEMENT_CREATE` |
| PUT | `/movement/update/{id}` | `MOVEMENT_UPDATE` |
| DELETE | `/movement/delete/{id}` | `MOVEMENT_DELETE` |

**Imagens:** no cadastro (`POST /product/add`) o arquivo é salvo em `file.upload-dir` com o nome `<uuid>_<nome-original>` (só o nome-base é usado e o prefixo evita que dois produtos com o mesmo nome de arquivo se sobrescrevam). O campo `image` do produto guarda esse nome, e `GET /product/image/{arquivo}` devolve o arquivo. O endpoint exige autenticação (uma tag `<img>` não envia o token, então o frontend baixa a imagem via requisição autenticada), só serve arquivos de imagem dentro da pasta de upload e responde `404` para qualquer outro caminho (incluindo tentativas de `../`).

As listagens aceitam paginação (`?page=0&size=10&sort=name,asc`).

## Problemas conhecidos

Já existiam antes do controle de acesso e continuam como estavam:

- `GET /product/{id}` e `GET /supplier/{id}` retornam erro 500, porque existe outro `GET` com o mesmo padrão de URL (`/product/{category}` e `/supplier/{name}`), e o Spring não consegue escolher o handler. Por isso eles não aparecem nas tabelas acima, embora tenham `@PreAuthorize` configurado.
- `GET /categories/{name}` com um nome inexistente retorna 500 (o mapper recebe `null`).
- `IllegalArgumentException` lançada pelos services (estoque insuficiente, nome duplicado) não tem handler e vira 500.

## Migração de bases existentes

Com `spring.jpa.hibernate.ddl-auto=update`, a coluna `role` em `tb_users` e a tabela `tb_user_permissions` são criadas automaticamente. Contas que já existiam ficam sem role gravada e são tratadas como `USER`, ou seja, **sem nenhuma permissão**. Para recuperar o acesso:

1. suba a aplicação com `SUPERUSER_EMAIL` apontando para uma conta existente (ela é promovida a Super Usuário) ou com `SUPERUSER_PASSWORD` para criar uma nova;
2. entre com o Super Usuário e use os endpoints de `/users` para promover ou conceder permissões às demais contas.

## Testes

```bash
./mvnw test
```

Os testes não precisam do PostgreSQL: os de integração usam H2 em memória (profile `test`, `src/test/resources/application-test.properties`, que também define o Super Usuário de teste).

| Tipo | Classes |
|---|---|
| Unitários (Mockito) | `*ServiceTest`, `TokenConfigTest`, `UserModelTest`, `SuperUserInitializerTest` |
| Controllers (`@WebMvcTest`, com a segurança real) | `*ControllerTest` |
| Integração (`@SpringBootTest` + H2) | `InventoryFlowIntegrationTest` (CRUD e estoque), `RolesAndPermissionsIntegrationTest` (roles e permissões) |

Observação para novos testes de controller: estenda `ControllerTestSupport` e autentique as requisições com `.with( auth() )` (todas as permissões) ou `.with( auth( Permission.X ) )` (apenas as informadas).
