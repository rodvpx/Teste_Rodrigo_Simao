package org.example;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class ProcessarDados {

    public void processar() {
        String url = "jdbc:sqlite:despesas.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            criarTabela(conn);

            Path dir = Paths.get("src/main/java/org/docs");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
                for (Path arquivo : stream) {
                    processarArquivo(arquivo, conn);
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void criarTabela(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS despesas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT,
                valor REAL,
                data TEXT
            )
            """;
        conn.createStatement().execute(sql);
    }

    private void processarArquivo(Path arquivo, Connection conn) throws IOException, SQLException {
        try (Reader reader = Files.newBufferedReader(arquivo);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            String[] cabecalho = csvReader.readNext(); // Primeira linha: colunas
            if (cabecalho == null) return;

            // Encontra índice da coluna relevante
            int indiceTipo = -1, indiceValor = -1, indiceData = -1;
            for (int i = 0; i < cabecalho.length; i++) {
                String col = cabecalho[i].trim().toLowerCase();
                if (col.contains("tipo") || col.contains("descrição")) indiceTipo = i;
                if (col.contains("valor")) indiceValor = i;
                if (col.contains("data")) indiceData = i;
            }

            if (indiceTipo == -1) return; // Sem coluna para filtrar

            // Bulk insert preparado
            String sql = "INSERT INTO despesas (tipo, valor, data) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);

                String[] linha;
                boolean temDespesas = false;
                int count = 0;

                while ((linha = csvReader.readNext()) != null) {
                    if (linha.length > indiceTipo &&
                            linha[indiceTipo].trim().contains("Despesas com Eventos/Sinistros")) {

                        pstmt.setString(1, linha[indiceTipo]);
                        pstmt.setString(3, indiceData < linha.length ? linha[indiceData] : "");

                        try {
                            pstmt.setDouble(2, indiceValor < linha.length ?
                                    Double.parseDouble(linha[indiceValor].replace(",", ".")) : 0);
                        } catch (NumberFormatException e) {
                            pstmt.setDouble(2, 0);
                        }

                        pstmt.addBatch();
                        temDespesas = true;
                        count++;

                        if (count % 1000 == 0) {
                            pstmt.executeBatch();
                            pstmt.clearBatch();
                        }
                    }
                }

                if (count % 1000 != 0) {
                    pstmt.executeBatch();
                }

                conn.commit();
                if (temDespesas) {
                    System.out.println("Processou " + count + " despesas de " + arquivo.getFileName());
                }
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

}
