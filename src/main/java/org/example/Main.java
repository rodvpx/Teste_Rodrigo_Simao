package org.example;

public class Main {
    public static void main(String[] args) {

        // Baixa os arquivos e extrai arquivos .zip
        new DownloadDocs().executar();

        //Processar dados
        new ProcessarDados().processar();
    }
}
