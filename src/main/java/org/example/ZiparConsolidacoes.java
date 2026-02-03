package org.example;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZiparConsolidacoes {

    public void zipar() {
        Path dir = Paths.get("src/main/java/org/docs");
        Path arquivoFonte = dir.resolve("consolidado_despesas");
        Path arquivoZip = dir.resolve("consolidado_despesas.zip");

        try (FileOutputStream fos = new FileOutputStream(arquivoZip.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(arquivoFonte.toFile())) {

            ZipEntry zipEntry = new ZipEntry(arquivoFonte.getFileName().toString());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            System.out.println("Arquivo zipado com sucesso em: " + arquivoZip);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
