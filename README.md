# Plotter

O Plotter é um aplicativo Android desenvolvido para desenhar até dois graficos simultâneos recebidos como string por meio do Bluetooth,
sendo possivel gravar tanto a tela quanto o conjunto de dados.

## Getting Started

Faça do download do projeto

https://github.com/luscasos/Plotter

### Requisitos


```
 Android Studio com Gradle superior a 4.3
```
```
 Celular com android superior a 4.4 
```

### Installing


```
Extraia a o arquivo baixado, caso o tenha feito por meio do .Zip.
```

```
No Android Studio clique em "Open an existing Android Studio project".
```

```
Selecione o caminho da pasta extraida.
```

## Deployment

```
Basta clicar em "Run" e selecionar o aparelho android conectado via USB
```

## Fluxo de dados

A conexão com o Bluetooth é feita apos a obtenção do MAC do dispositivo dando inicio ao service (Linha 307 GraphActivity.java), que é direcionado para o onStartCommand no service.java que liga o Bluetooth, se iniciado com sucesso o recebimento de dados é dado no Handler (Linha 91, service.java) onde deve ser tratado o tamanho da string recebida que sera retornada ao GraphActivity.java por meio da variavel string "dado".
O tratamento da String para plot no GraphActivity.java ocorre no Handler (linha 185).

