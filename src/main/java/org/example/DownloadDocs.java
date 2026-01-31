package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadDocs {
    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/";
    private static final String DOWNLOAD_DIR = "src/main/java/org/docs";

    public void executar() {
        try {
            // 1. Acessar a URL base e encontrar o link para "demonstracoes_contabeis"
            System.out.println("Conectando a: " + BASE_URL);
            String demonstracoesUrl = findLinkByText(BASE_URL, "demonstracoes_contabeis");

            if (demonstracoesUrl == null) {
                // Tenta procurar com acento caso não ache sem
                demonstracoesUrl = findLinkByText(BASE_URL, "demonstrações contábeis");
            }

            if (demonstracoesUrl == null) {
                System.out.println("Pasta 'demonstracoes_contabeis' não encontrada.");
                return;
            }

            System.out.println("Acessando: " + demonstracoesUrl);

            // 2. Dentro de demonstracoes_contabeis, achar a pasta com a data mais recente
            String latestDateUrl = findLatestDateFolder(demonstracoesUrl);
            if (latestDateUrl == null) {
                System.out.println("Nenhuma pasta de data encontrada.");
                return;
            }

            System.out.println("Pasta mais recente encontrada: " + latestDateUrl);

            // 3. Baixar os arquivos ZIP dessa pasta
            downloadZipsFromUrl(latestDateUrl);

            // 4. Descompactar os arquivos baixados
            descompactarArquivosBaixados();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String findLinkByText(String url, String partialText) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            // Verifica se o href ou o texto do link contém o termo procurado
            if (href.toLowerCase().contains(partialText.toLowerCase()) ||
                    link.text().toLowerCase().contains(partialText.toLowerCase())) {
                return resolveUrl(url, href);
            }
        }
        return null;
    }

    private String findLatestDateFolder(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        List<String> folders = new ArrayList<>();

        for (Element link : links) {
            String href = link.attr("href");
            // Ignora link para diretório pai
            if (href.equals("../") || href.equals("/")) continue;

            // Assume que pastas terminam com /
            if (href.endsWith("/")) {
                folders.add(href);
            }
        }

        // Ordena decrescente para pegar a "maior" string (datas YYYY-MM ou)
        if (folders.isEmpty()) return null;

        Collections.sort(folders, Collections.reverseOrder());
        String latestFolder = folders.get(0);

        return resolveUrl(url, latestFolder);
    }

    private void downloadZipsFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");

        Path downloadPath = Paths.get(DOWNLOAD_DIR);
        if (!Files.exists(downloadPath)) {
            Files.createDirectories(downloadPath);
        }

        int count = 0;
        for (Element link : links) {
            String href = link.attr("href");
            if (href.toLowerCase().endsWith(".zip")) {
                String fileUrl = resolveUrl(url, href);
                String fileName = href.substring(href.lastIndexOf('/') + 1);

                System.out.println("Baixando: " + fileName);
                try (InputStream in = new URL(fileUrl).openStream()) {
                    Files.copy(in, downloadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Salvo em: " + downloadPath.resolve(fileName));
                    count++;
                } catch (IOException e) {
                    System.err.println("Erro ao baixar " + fileName + ": " + e.getMessage());
                }
            }
        }

        if (count == 0) {
            System.out.println("Nenhum arquivo .zip encontrado em " + url);
        } else {
            System.out.println("Total de arquivos baixados: " + count);
        }
    }

    private void descompactarArquivosBaixados() throws IOException {
        Path dir = Paths.get(DOWNLOAD_DIR);
        if (!Files.exists(dir)) return;

        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path entry : stream) {
                System.out.println("Descompactando: " + entry.getFileName());
                unzip(entry, dir);
                Files.delete(entry);
            }
        }
    }

    private void unzip(Path zipFilePath, Path destDir) throws IOException {
        File dir = destDir.toFile();
        // buffer para leitura
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(dir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Falha ao criar diretório " + newFile);
                    }
                } else {
                    // Garante que o diretório pai existe
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Falha ao criar diretório " + parent);
                    }
                    // Escreve o arquivo
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    // Método de segurança para evitar vulnerabilidade "Zip Slip"
    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entrada zip está fora do diretório de destino: " + zipEntry.getName());
        }

        return destFile;
    }

    private String resolveUrl(String baseUrl, String href) {
        if (href.startsWith("http")) return href;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        return baseUrl + href;
    }
}
