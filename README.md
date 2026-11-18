# Sistema de Controlo de Produção da Acme

[![Testes automatizados](https://github.com/Ivany23/Acme/actions/workflows/testes.yml/badge.svg)](https://github.com/Ivany23/Acme/actions/workflows/testes.yml)

| Parte | Tecnologia | Onde corre |
|---|---|---|
| `frontend/` | React 19 + Vite | **Vercel** |
| `backend/` | Spring Boot 4.1 (Java 21), API REST | **Render** (`https://sistema-acme-api.onrender.com`) |
| Base de dados | PostgreSQL | **Neon** |

Padrão de projecto: **Observer** (`acme.observer`). A `Encomenda` (sujeito) avisa o `Cliente`, que guarda
as notificações e as vê na área de cliente, e o `GeradorRelatorio`, que cria o relatório quando a
encomenda é concluída (RF009).

## Entrar no sistema

| Endereço | Para quem |
|---|---|
| `/entrar` | Funcionários da Acme |
| `/cliente/entrar` | Clientes (entram com o email) |
| `/cliente/registar` | Cliente novo cria a sua conta |

**Contas de demonstração** (password `acme2026` em todas), uma por actor do TG1:

| Login | Perfil | O que pode fazer |
|---|---|---|
| `admin` | Administrador | Tudo, incluindo gerir utilizadores |
| `director` | Director-Geral | Toda a gestão, excepto utilizadores |
| `contador` | Contador | Materiais, peças e custos; confirma pagamentos; relatórios e análise |
| `supervisor` | Supervisor de Turno | Turnos e alocação; produção e ponto; conclui encomendas; relatórios |
| `vendas` | Gerente de Vendas | Clientes, orçamentos e encomendas; consulta o histórico |
| `artesao` | Artesão (João Mondlane) | Regista a **sua** produção e o **seu** ponto |
| `ana@mail.com` | Cliente (Ana Machava), entra em `/cliente/entrar` | Área de cliente: orçamento, encomendas, estado e notificações |

O administrador cria mais contas em **Utilizadores**. Cada pessoa altera a sua password no botão
**Password**. A API recusa (403) tudo o que o perfil não permite, mesmo que se tente pelo endereço.

## Rastreabilidade com o TG1

| TG1 | Implementação |
|---|---|
| RF001 Registo de peças | `Peca`, `EtapaProducao`, `MaterialPeca` · página Peças |
| RF002 Registo de encomendas | `Encomenda` · página Encomendas e área de cliente |
| RF003 Registo de funcionários | `Funcionario`, `Artesao` · página Funcionários |
| RF004 Registo de turnos | `Turno` · página Turnos |
| RF005 Alocação automática | `GestorAlocacao` · botão "Alocar automaticamente" |
| RF006 Registo de produção | `RegistoProducao` · página Registo de produção |
| RF007 Entradas e saídas | `RegistoPonto` (horas extra) · página Registo de ponto |
| RF008 Cálculo de custos | `CalculadoraCustos` · orçamento na encomenda e na área de cliente |
| RF009 Relatórios | `GeradorRelatorio` (Observer), `RelatorioProducao`, `AnalisadorDados` com critérios de análise |
| Actor Cliente: Registar Conta, Autenticar, Solicitar Orçamento, Acompanhar Estado | `ServicoAutenticacao`, `PortalClienteService` · `/cliente` |
| Actores Contador, Director, Supervisor, Gerente de Vendas, Artesão | `Papel` + `@Acesso` em cada endpoint |
| Cartão CRC ServicoAutenticacao | `acme.servico.ServicoAutenticacao` |

**RNF003 e RNF006** são requisitos de infra-estrutura. No alojamento académico actual:
- **RNF003**: todo o tráfego usa HTTPS (TLS) através do Render e da Vercel, e as passwords ficam cifradas
  com BCrypt. A VPN e a autenticação de dois factores exigem a rede corporativa da Acme e não estão
  implementadas.
- **RNF006**: a base de dados Neon está nos EUA (us-east-2). Para cumprir a região UE (Frankfurt), criam-se
  a base de dados Neon e o serviço do Render na região Frankfurt, sem alterar o código.

## Testes automatizados (140)

Correm **sozinhos no GitHub Actions** em cada `push` e `pull request` (separador *Actions*).
Localmente, `testar.bat` corre tudo.

| Tipo | Backend (JUnit 5) | Frontend (Vitest) | O que verifica |
|---|---|---|---|
| **Unidade** | `acme.unidade` (51) | `testes/unidade` (13) | Entidades, Observer, custos, tokens, regras de acesso, critérios de análise; formatação e permissões |
| **Serviços** | `acme.servicos` (27) | `testes/servicos` (6) | Serviços com dependências simuladas (Mockito); cliente HTTP do frontend |
| **Integração** | `acme.integracao` (23) | `testes/integracao` (10) | API + base de dados H2 (MockMvc): perfis, portal do cliente, segurança; páginas React com a API |
| **Sistema** | `acme.sistema` (4) | `testes/sistema` (6) | Aplicação completa num servidor HTTP real; navegação por todas as telas e por perfil |

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
- **Frontend (Vercel)**: importe o repositório sem alterar nada, porque o `vercel.json` já indica como
  compilar a pasta `frontend/`. O endereço da API está em `frontend/.env.production`.

## Variáveis de ambiente do backend
| Variável | Descrição | Por omissão |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` | obrigatória |
| `DB_USER` / `DB_PASSWORD` | credenciais da Neon | obrigatórias |
| `CORS_ORIGINS` | origens do frontend (aceita `*`) | `http://localhost:5173,https://*.vercel.app` |
| `AUTH_SEGREDO` | segredo que assina as sessões | derivado da password da BD |
| `ADMIN_LOGIN` / `ADMIN_PASSWORD` | primeiro administrador (e password das contas de demonstração) | `admin` / `acme2026` |
| `PERCENTAGEM_ORGANIZACAO` | margem no orçamento (%) | `20` |
| `DADOS_INICIAIS` | dados e contas de demonstração | `true` |

## Endpoints (`/api`)
Todos exigem `Authorization: Bearer <token>`, excepto `POST /auth/login`, `POST /portal/registar` e `GET /estado`.

| Recurso | Métodos |
|---|---|
| `POST /auth/login`, `GET /auth/eu`, `PUT /auth/password` | sessão e password |
| `/utilizadores`, `/utilizadores/papeis` | contas de acesso (administrador) |
| `/portal/registar`, `/portal/pecas`, `/portal/orcamento`, `/portal/encomendas`, `/portal/encomendas/{id}/cancelar`, `/portal/notificacoes` | área de cliente |
| `/materiais`, `/pecas` | CRUD (RF001) |
| `/clientes`, `/clientes/{id}/notificacoes` | CRUD + notificações (Observer) |
| `/funcionarios`, `/funcionarios/artesaos` | CRUD (RF003) |
| `/turnos`, `/alocacoes?data=`, `POST /alocacoes/automatica` | turnos e alocação (RF004, RF005) |
| `/encomendas`, `GET /encomendas/orcamento`, `POST /encomendas/{id}/pagamento`, `PATCH /encomendas/{id}/estado` | encomendas e custos (RF002, RF008) |
| `/producao`, `/ponto` | produção e ponto (RF006, RF007) |
| `/relatorios?inicio=&fim=&pecaId=&artesao=`, `/relatorios/analise?…`, `/dashboard` | relatórios e análise com critérios (RF009) |
