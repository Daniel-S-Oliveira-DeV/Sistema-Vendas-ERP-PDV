# 🛒 Sistema de Vendas e Controle de Estoque — ERP & Frente de Caixa (PDV)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-2196F3?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-100%25_Offline-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Status](https://img.shields.io/badge/Status-Produção_v1.0-2ed573?style=for-the-badge)

Sistema Desktop completo de **Gestão Empresarial (ERP) e Frente de Caixa (PDV)** desenvolvido em **Java 21 + JavaFX**, com banco de dados relacional embarcado (**SQLite**) e arquitetura **100% offline**. 

Projetado com suporte **White-Label**, o sistema permite que qualquer comércio (mercados, boutiques, lojas de informática, autopeças ou conveniências) personalize o nome da empresa e o CNPJ/CPF diretamente pela interface, refletindo automaticamente nas telas, recibos térmicos e relatórios gerenciais.

---

## ✨ Principais Funcionalidades

* **🏷️ Sistema White-Label (Personalizável):** O proprietário define o Nome Fantasia e o CNPJ/CPF da loja nas configurações, atualizando a tela de login, o menu principal, o cabeçalho dos relatórios A4 e os cupons de venda.
* **🔐 Controle de Acesso e Gestão de Usuários:** Tela de login segura com criação de múltiplos operadores, alteração de senhas e separação por níveis de acesso (Administrador e Operador/Caixa).
* **🔑 Gestão de Turno e Gaveta de Caixa:** Bloqueio de segurança que impede vendas com o caixa fechado. Registro completo de abertura de turno (fundo de troco), **Sangrias** (retiradas), **Suprimentos** (reforço de troco) e conferência cega/assistida no fechamento.
* **⚡ Frente de Caixa (PDV) Ágil com Atalhos de Teclado:**
  * Busca inteligente por **Código de Barras (Leitor EAN)** ou autocompletar por nome.
  * Atalhos rápidos para operação sem mouse (`F1` Buscar, `F2` Cancelar, `F3` Desconto, `F4` Quantidade, `F5` Estorno, `F12` Concluir Venda, `DEL` Remover item).
  * Múltiplas formas de pagamento: Dinheiro, PIX, Cartão de Crédito, Cartão de Débito e **Fiado (Caderninho)**.
* **📒 Controle de Fiado, Limite de Crédito e Inadimplência:**
  * Trava automática de limite de crédito disponível por cliente.
  * Cálculo opcional de juros (%) e definição de data de vencimento.
  * **Alerta de Inadimplência Automático:** Ao abrir o painel principal, o sistema verifica e alerta visualmente sobre contas de fiado vencidas.
* **🔄 Histórico e Estorno de Vendas (`F5`):** Cancelamento seguro de vendas com *rollback* transacional (devolve automaticamente os itens ao estoque, registra entrada no Kardex, cancela o lançamento no financeiro e restaura o limite de crédito do cliente).
* **🧾 Emissão de Cupom Não-Fiscal (80mm):** Geração automática de recibo em PDF formatado para bobina térmica de 80mm com nome e CNPJ da loja, itens, descontos, juros e identificação do cliente no nome do arquivo.
* **📊 Relatórios Gerenciais em PDF (OpenPDF):**
  * **Relatório Detalhado de Vendas, Custos e Lucro:** Lista itens vendidos, preço de venda, custo unitário, Custo das Mercadorias Vendidas (CMV) e **Lucro Líquido** obtido por venda e no período.
  * **Relatório de Perdas e Ajustes:** Detalhamento de baixas manuais com custo unitário, custo total perdido (prejuízo de estoque) e receita cessante.
  * **Relatórios de Fluxo de Caixa, Movimentação de Estoque (Kardex) e Fiados Consolidados.**
* **💾 Backup e Restauração 1-Click:** Exportação e importação do banco de dados para pendrive ou nuvem diretamente pela tela de configurações.

---

## 📥 Download e Instalação (Para Usuários Finais)

Você não precisa instalar bancos de dados externos. O sistema cria a estrutura limpa automaticamente na primeira abertura.

### Opção 1: Windows (`.exe`)
1. Acesse a aba [**Releases**](../../releases) deste repositório.
2. Baixe o arquivo **`SistemaVendasERP.exe`**.
3. Coloque o executável em uma pasta de sua preferência (ex: `C:\SistemaVendas\`).
4. Certifique-se de ter o **Java 21 (JRE ou JDK)** instalado no computador (ou coloque a pasta `jre` portátil ao lado do `.exe`).
5. Dê dois cliques em **`SistemaVendasERP.exe`** para iniciar!

### Opção 2: Linux ou Windows (`.jar` Universal)
1. Baixe o arquivo **`SistemaVendasERP.jar`** na aba [**Releases**](../../releases).
2. Com o Java 21 instalado, abra o terminal na pasta do arquivo e execute:
   ```bash
   java -jar SistemaVendasERP.jar
   ```

---

## 🔑 Primeiro Acesso e Recomendações de Segurança

Na primeira vez que o sistema é aberto, ele gera um banco de dados limpo (`banco_pdv.db`) com uma conta administrativa padrão:

* **Usuário inicial:** `admin`
* **Senha inicial:** `admin`

### 🛡️ O que fazer imediatamente após o primeiro login:
1. **Personalize o Nome da sua Empresa (White-Label):**
   * No menu lateral, clique em **Configurações**.
   * No painel *Personalização do Sistema (White-Label)*, digite o **Nome da sua Loja** e o **CNPJ ou CPF** e clique em **Aplicar Novo Nome ao Sistema**.
2. **Altere a Senha Padrão do `admin`:**
   * Ainda na tela de **Configurações**, role até a seção **Gerenciamento de Usuários e Controle de Acesso**.
   * Clique sobre o usuário `admin` na tabela (ou digite `admin` no campo de login), insira uma **nova senha forte** e clique em **Salvar / Atualizar Usuário**.
3. **Cadastre os Operadores de Caixa:**
   * Digite o login do funcionário (ex: `caixa01`), defina a senha, selecione o nível **2 - Operador / Vendedor** e clique em **Salvar / Atualizar Usuário**.

---

## 📖 Guia Rápido de Uso Diário

1. **Abertura do Dia (Turno):** Vá em **Controle de Caixa (Turno)**, informe o valor em dinheiro disponível na gaveta para troco (ex: `50,00`) e clique em **Abrir Caixa**.
2. **Cadastro de Produtos e Clientes:**
   * Em **Produtos e Estoque**, cadastre os itens informando código de barras, custo, preço de venda e estoque inicial.
   * Em **Clientes**, cadastre os clientes e defina o **Limite de Crédito (R$)** para compras no caderninho (fiado).
3. **Realizando Vendas:**
   * Acesse **Frente de Caixa (PDV)**, bipe o código de barras ou digite o nome do produto.
   * Escolha a forma de pagamento e pressione **`F12`** (ou clique em *Concluir*). O cupom em PDF será gerado e aberto automaticamente na pasta `Documentos/TechStoreERP_Relatorios/Cupons`.
4. **Fechamento do Dia:**
   * Ao final do expediente, volte em **Controle de Caixa (Turno)**, confira o valor esperado na gaveta, digite o valor físico contado e clique em **Fechar Caixa**.
   * Vá em **Configurações** e clique em **Exportar Backup** para salvar uma cópia de segurança no seu pendrive.

---

## 🛠️ Como Compilar o Projeto (Para Desenvolvedores)

Caso deseje clonar o código-fonte, editar e gerar um novo `.exe` e `.jar`:

```bash
# 1. Clone este repositório
git clone [https://github.com/daniel-souza-de-oliveira/NOME-DO-SEU-REPOSITORIO.git](https://github.com/daniel-souza-de-oliveira/NOME-DO-SEU-REPOSITORIO.git)
cd NOME-DO-SEU-REPOSITORIO

# 2. Execute em modo de desenvolvimento
mvn clean javafx:run

# 3. Gere o pacote final de produção (.jar e .exe na pasta target/)
mvn clean package
```

---

## 👨‍💻 Autor

Desenvolvido por **Daniel Souza de Oliveira**  
[![GitHub](https://img.shields.io/badge/GitHub-daniel--souza--de--oliveira-181717?style=flat-square&logo=github)](https://github.com/daniel-souza-de-oliveira)