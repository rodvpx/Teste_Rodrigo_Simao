package org.example;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ProcessarDados {

    public void processar() {
        Path dir = Paths.get("src/main/java/org/docs");
        Path arquivoSaida = dir.resolve("consolidado_despesas");

        // Cria o arquivo de saída e escreve o cabeçalho
        try (CSVWriter writer = new CSVWriter(new FileWriter(arquivoSaida.toFile()))) {
            writer.writeNext(new String[]{"CNPJ", "RazaoSocial", "Trimestre", "Ano", "ValorDespesas"});

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
                for (Path arquivo : stream) {
                    // Ignora o arquivo de saída se ele estiver na mesma pasta
                    if (arquivo.getFileName().toString().equals("consolidado_despesas")) continue;
                    
                    System.out.println("Processando arquivo: " + arquivo.getFileName());
                    processarArquivo(arquivo, writer);
                }
            }
            System.out.println("Consolidação concluída em: " + arquivoSaida);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processarArquivo(Path arquivo, CSVWriter writer) {

        try (Reader reader = Files.newBufferedReader(arquivo, StandardCharsets.ISO_8859_1);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                     .build()) {

            String[] cabecalho = csvReader.readNext();
            if (cabecalho == null) return;

            int idxDescricao = -1;
            int idxValor = -1;
            int idxData = -1;
            int idxRegAns = -1;

            // Identifica índices das colunas
            for (int i = 0; i < cabecalho.length; i++) {
                String col = cabecalho[i].trim().toUpperCase();
                if (col.contains("DESCRICAO")) idxDescricao = i;
                else if (col.contains("VL_SALDO_FINAL")) idxValor = i;
                else if (col.contains("DATA")) idxData = i;
                else if (col.contains("REG_ANS")) idxRegAns = i;
            }

            if (idxDescricao == -1) {
                System.out.println("Coluna DESCRICAO não encontrada em " + arquivo.getFileName());
                return;
            }

            String[] linha;
            while ((linha = csvReader.readNext()) != null) {
                // Verifica se a linha tem colunas suficientes e se a descrição corresponde
                if (linha.length > idxDescricao && 
                    linha[idxDescricao].contains("Despesas com Eventos/Sinistros")) {
                    
                    String dataStr = (idxData != -1 && linha.length > idxData) ? linha[idxData] : "";
                    String valorStr = (idxValor != -1 && linha.length > idxValor) ? linha[idxValor] : "0";
                    String regAns = (idxRegAns != -1 && linha.length > idxRegAns) ? linha[idxRegAns] : "";

                    // Processa data para obter Trimestre e Ano
                    String trimestre = "";
                    String ano = "";
                    if (!dataStr.isEmpty()) {
                        try {
                            LocalDate data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            ano = String.valueOf(data.getYear());
                            trimestre = String.valueOf((data.getMonthValue() - 1) / 3 + 1);
                        } catch (Exception e) {
                            // Data inválida, mantém vazio
                        }
                    }

                    // Formata valor (substitui vírgula por ponto se necessário)
                    String valor = valorStr.replace(",", ".");

                    // Escreve linha no CSV consolidado
                    // Como não temos CNPJ e Razão Social nos arquivos, usamos REG_ANS no lugar de CNPJ e deixamos Razão Social vazia
                    writer.writeNext(new String[]{regAns, "", trimestre, ano, valor});
                }
            }

        } catch (IOException | CsvValidationException e) {
            System.err.println("Erro ao ler arquivo " + arquivo + ": " + e.getMessage());
        }
    }
}
