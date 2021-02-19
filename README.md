# TUBES 1 STIMA

## Penjelasan Singkat
Bot 13519119_Botch dibuat dengan implementasi algoritma greedy. Bot akan membaca game state dari state.json. Bot mengenali semua command yang mungkin untuk dilakukan (Himpunan kandidat), kemudian bot akan menilai kelayakan dari semua command yang mungkin satu persatu. Setelah itu bot akan menyeleksi command yang mana yang harus dilaksanakan sesuai prioritas yang telah kami tentukan. Setelah itu, bot akan mempassing nilai string berisi command yang valid dan sesuai ke game engine untuk dijalankan commandnya.


## Requirement:
1. Java (minimal Java 8)
2. IntelIJ IDEA
3. NodeJS

## Cara Menggunakan

1. Extract file Tubes1_13519119.zip ke dalam folder starter-pack tempat menjalankan permainan "Worms". starter-pack dapat di unduh dari link berikut : https://github.com/EntelectChallenge/2019-Worms/releases/tag/2019.3.2 

2. Atur pemain/bot dan bot yang akan di lawan dengan cara mengganti nilai varriabel player yang ada di game-runner-config.json menjadi path menuju folder yang berisi bot.json\
misalnya :\
{\
  ......\
  "player-a": "./Tubes1_13519127",\
  "player-b": "./reference-bot/javascript",\
  .....\
}\
 untuk kasus jika folder di extract di "./starter-pack"

3. Jalankan permainan "Worms" dengan cara double-click run.bat.

## Author/Identitas Pembuat :
Giant Andreas Tambunan - 13519127@std.stei.itb.ac.id\
Reynard Hans Prayoga - 13519119@std.stei.itb.ac.id\
Project Link: https://github.com/giantandreas/Tubes1-Stima
