# Sistema de Controlo de Produção da Acme

[![Testes automatizados](https://github.com/Ivany23/Acme/actions/workflows/testes.yml/badge.svg)](https://github.com/Ivany23/Acme/actions/workflows/testes.yml)

| Parte | Tecnologia | Onde corre |
|---|---|---|
| `frontend/` | React 19 + Vite | **Vercel** |
| `backend/` | Spring Boot 4.1 (Java 21), API REST | **Render** (`https://sistema-acme-api.onrender.com`) |
| Base de dados | PostgreSQL | **Neon** |

Padrão de projecto: **Observer** (`acme.observer`). A `Encomenda` (sujeito) avisa o `Cliente`, que guarda
as notificações, e o `GeradorRelatorio`, que cria o relatório quando a encomenda é concluída (RF009).

**Login por omissão:** utilizador `admin`, password `acme2026` (alterável com `ADMIN_LOGIN` / `ADMIN_PASSWORD`
antes do primeiro arranque).

## Testes automatizados

Os testes correm **sozinhos no GitHub Actions** em cada `push` e `pull request` (ver o separador *Actions* do
repositório). Localmente, `testar.bat` corre tudo.

| Tipo | Backend (JUnit 5) | Frontend (Vitest) | O que verifica |
|---|---|---|---|
| **Unidade** | `acme.unidade` (36) | `testes/unidade` (6) | Classes isoladas: regras das entidades, Observer, cálculo de custos, tokens; formatação de valores |
| **Serviços** | `acme.servicos` (19) | `testes/servicos` (6) | Camada de serviços com dependências simuladas (Mockito); cliente HTTP do frontend |
| **Integração** | `acme.integracao` (10) | `testes/integracao` (6) | Controlador → serviço → repositório → base de dados H2 (MockMvc); páginas React com a API |
| **Sistema** | `acme.sistema` (2) | `testes/sistema` (3) | Aplicação completa num servidor HTTP real, cenário de ponta a ponta; navegação por todas as telas |

Para correr só um tipo:
```
cd backend  && mvnw.cmd test -Dtest="acme.unidade.**"      (ou servicos, integracao, sistema)
cd frontend && npx vitest run testes/unidade               (ou servicos, integracao, sistema)
```

## Correr localmente
1. Copie `backend/.env.example` para `backend/.env` e preencha `DB_URL`, `DB_USER` e `DB_PASSWORD`.
2. `executar.bat` abre a API (http://localhost:8080) e o frontend (http://localhost:5173).

## Deploy
- **Backend (Render)**: *New → Blueprint* com este repositório usa o `render.yaml`. Preencha `DB_URL`,
  `DB_USER` e `DB_PASSWORD`. Cada `push` no `main` faz novo deploy.
- **Frontend (Vercel)**: *Add New → Project* e importe este repositório **sem alterar nada**, porque o
  `vercel.json` na raiz já indica como compilar a pasta `frontend/`. O endereço da API está em
  `frontend/.env.production`. Os domínios `*.vercel.app` já estão autorizados no CORS da API.

## Variáveis de ambiente do backend
| Variável | Descrição | Por omissão |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` | obrigatória |
| `DB_USER` / `DB_PASSWORD` | credenciais da Neon | obrigatórias |
| `CORS_ORIGINS` | origens do frontend (aceita `*`) | `http://localhost:5173,https://*.vercel.app` |
| `AUTH_SEGREDO` | segredo que assina as sessões | derivado da password da BD |
| `ADMIN_LOGIN` / `ADMIN_PASSWORD` | primeiro utilizador | `admin` / `acme2026` |
| `PERCENTAGEM_ORGANIZACAO` | margem no orçamento (%) | `20` |
| `DADOS_INICIAIS` | inserir dados de demonstração se a BD estiver vazia | `true` |

## Endpoints (`/api`)
Todos exigem `Authorization: Bearer <token>`, excepto `POST /auth/login` e `GET /estado`.

| Recurso | Métodos |
|---|---|
| `POST /auth/login`, `GET /auth/eu` | login e utilizador actual |
| `/materiais`, `/pecas` | CRUD (RF001) |
| `/clientes`, `/clientes/{id}/notificacoes` | CRUD + notificações (Observer) |
| `/funcionarios`, `/funcionarios/artesaos` | CRUD (RF003) |
| `/turnos`, `/alocacoes?data=`, `POST /alocacoes/automatica` | turnos e alocação (RF004, RF005) |
| `/encomendas`, `GET /encomendas/orcamento`, `POST /encomendas/{id}/pagamento`, `PATCH /encomendas/{id}/estado` | encomendas e custos (RF002, RF008) |
| `/producao`, `/ponto` | produção e ponto (RF006, RF007) |
| `/relatorios`, `/relatorios/analise`, `/dashboard` | relatórios e análise (RF009) |
