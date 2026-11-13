# Sistema de Controlo de Produção da Acme

- `backend/`: API REST em **Spring Boot 4.1 (Java 21)** com **PostgreSQL (Neon)**
- `frontend/`: interface em **React** (próximo passo), a publicar na **Vercel**

Padrão de projecto: **Observer** (pacote `acme.observer`)
- Sujeito: `Encomenda` avisa os observadores quando muda de estado.
- Observadores: `Cliente` (guarda as notificações) e `GeradorRelatorio` (gera o relatório
  automaticamente quando a encomenda é concluída, RF009).

## Correr localmente
1. Copie `backend/.env.example` para `backend/.env` e preencha `DB_URL`, `DB_USER` e `DB_PASSWORD`.
2. `executar.bat` arranca a API em http://localhost:8080 (as tabelas são criadas automaticamente).
3. `testar.bat` corre os 35 testes: unitários e integração da API com H2 em memória, sem precisar da Neon.

Na primeira execução com a base de dados vazia são inseridos dados de demonstração
(desligar com `DADOS_INICIAIS=false`).

## Variáveis de ambiente
| Variável | Descrição | Por omissão |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` | obrigatória |
| `DB_USER` / `DB_PASSWORD` | credenciais da Neon | obrigatórias |
| `CORS_ORIGINS` | origens do frontend (aceita `*`) | `http://localhost:5173,https://*.vercel.app` |
| `PERCENTAGEM_ORGANIZACAO` | margem da organização no orçamento (%) | `20` |
| `DADOS_INICIAIS` | inserir dados de demonstração | `true` |
| `PORT` | porta HTTP | `8080` |

## Endpoints (`/api`)
| Recurso | Métodos |
|---|---|
| `/materiais`, `/materiais/{id}` | GET, POST, PUT, DELETE (RF001) |
| `/pecas`, `/pecas/{id}` | GET, POST, PUT, DELETE: com `materiais[]` e `etapas[]` (RF001) |
| `/clientes`, `/clientes/{id}`, `/clientes/{id}/notificacoes` | GET, POST, PUT, DELETE |
| `/funcionarios`, `/funcionarios/artesaos`, `/funcionarios/{id}` | GET, POST, PUT, DELETE: `tipo` = `ARTESAO` ou `FUNCIONARIO` (RF003) |
| `/turnos`, `/turnos/{id}` | GET, POST, PUT, DELETE (RF004) |
| `/alocacoes?data=`, `POST /alocacoes/automatica`, `POST /alocacoes`, `DELETE /alocacoes/{id}` | alocação de turnos (RF005) |
| `/encomendas?estado=&clienteId=`, `/encomendas/{id}` | GET, POST, DELETE (só pendentes) (RF002) |
| `GET /encomendas/orcamento?pecaId=&quantidade=&custosIndirectos=` | simulação do orçamento (RF008) |
| `POST /encomendas/{id}/pagamento` | paga e passa a `EM_PRODUCAO` |
| `PATCH /encomendas/{id}/estado` `{"estado":"CONCLUIDA"}` | muda o estado; ao concluir, gera o relatório |
| `/producao?encomendaId=`, `DELETE /producao/{id}` | registo de produção (RF006) |
| `/ponto?artesaoId=&data=`, `DELETE /ponto/{id}` | registo de ponto; sem `turnoId` usa o turno alocado (RF007) |
| `/relatorios`, `/relatorios/encomenda/{id}`, `/relatorios/analise` | relatórios e análise (RF009) |
| `/dashboard`, `/estado` | resumo geral e verificação da API |

Formatos: datas `AAAA-MM-DD` e horas `HH:mm`. Os erros vêm sempre como
`{status, erro, mensagem, campos}` (400 dados inválidos, 404 não encontrado, 409 regra de negócio).

Transições de estado: `PENDENTE → EM_PRODUCAO (após pagamento) → CONCLUIDA`; `PENDENTE/EM_PRODUCAO → CANCELADA`.

## Deploy
A **Vercel não executa Java/Spring Boot**. A arquitectura de produção fica assim:
- **Backend** no [Render](https://render.com) (plano gratuito, Docker): *New → Blueprint* usa o `render.yaml`;
  preencha `DB_URL`, `DB_USER` e `DB_PASSWORD` no painel.
- **Frontend React** na **Vercel**, com `VITE_API_URL=https://<sua-api>.onrender.com/api`.
- **Base de dados** na **Neon**.
