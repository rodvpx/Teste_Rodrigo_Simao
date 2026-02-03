# 1. TESTE DE INTEGRAÇÃO COM API PÚBLICA

Para a realização deste teste, realizei pesquisas na internet para compreender como abordar cada etapa. Utilizei o auxílio de inteligência artificial para implementar as funcionalidades com base nas pesquisas feitas, e, por fim, realizei ajustes manuais para refinar o código e garantir que a solução atendesse completamente aos requisitos.

Bom, de início pensei em fazer um projeto Java com Maven, sem usar frameworks. Para dependências, estamos utilizando o Maven com o arquivo `pom.xml`.

## 1.1. Acesso à API de Dados Abertos da ANS

Para acessar a API, utilizamos a biblioteca Jsoup para fazer o parsing do HTML da página de dados abertos da ANS. Navegamos pela estrutura da página para encontrar os links dos arquivos .zip, realizamos o download e, em seguida, descompactamos os arquivos CSV para o processamento.

## 1.2. Processamento de Arquivos

Decidimos processar os arquivos CSV lendo-os um a um. Para cada arquivo, os dados relevantes são extraídos e processados em memória antes de passar para a etapa de consolidação.

## 1.3. Consolidação e Análise de Inconsistências

A consolidação dos dados é feita em sequência ao processamento descrito no passo 1.2. Durante a análise, notamos que os arquivos não continham colunas explícitas para CNPJ e Razão Social. Para contornar essa limitação, decidimos utilizar os dados da coluna `CD_CONTA_CONTABIL` como substituto para o CNPJ e os dados da coluna `DESCRICAO` como substituto para a Razão Social.

# 2. TESTE DE TRANSFORMAÇÃO E VALIDAÇÃO DE DADOS

Pelo fato de os dados extraídos não conterem informações diretas de CNPJ e Razão Social das operadoras, não foi possível prosseguir com os desafios propostos na parte 2 do teste, que dependiam dessas informações para a busca de dados cadastrais.

# 3. Como Executar

Para executar a aplicação, siga os passos no arquivo `Main.java` (`src/main/java/org/example/Main.java`), comentando e descomentando as linhas conforme necessário.

### Passo 1: Download dos Arquivos
Para a execução inicial, mantenha apenas a linha a seguir descomentada:
```java
new DownloadDocs().executar();
```
Isso irá baixar os arquivos do site da ANS, extrair os arquivos `.csv` para a pasta `src/main/java/org/docs`, e apagar os arquivos `.zip` baixados.

### Passo 2: Processamento e Consolidação
Após o download, comente a linha do passo anterior e descomente a seguinte:
```java
new ProcessarDados().processar();
```
Este passo irá processar todos os arquivos `.csv` da pasta `docs` e gerar o arquivo `consolidado_despesas` no mesmo diretório.

### Passo 3: Compactação
Por fim, comente a linha do passo 2 e descomente a última linha:
```java
new ZiparConsolidacoes().zipar();
```
Isso irá compactar o arquivo `consolidado_despesas` em um arquivo `consolidado_despesas.zip` na mesma pasta, mantendo o arquivo original.
