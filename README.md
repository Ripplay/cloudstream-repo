# Oygy8b5F Cloudstream Repo

Cloudstream için 20 ayrı Türkçe film ve dizi kaynağı içeren, otomatik derlenen eklenti reposu.

## Kısa kod

24 Ağustos 2026 tarihinde etkinleştirilen kısa kod:

```text
Oygy8b5F
```

Cloudstream'de **Ayarlar > Eklentiler > Repo ekle** alanına bu kod yazılır. Cloudstream
alfanümerik kısa kodları `https://cutt.ly/<kod>` adresinden çözer. Bu kod aşağıdaki
repo tanımına yönlenir:

```text
https://raw.githubusercontent.com/Ripplay/cloudstream-repo/main/repo.json
```

Kısa kodun makine tarafından okunabilir tanımı `shortcode.json` dosyasındadır.

## Kaynaklar

1. HDFilmCehennemi — `https://www.hdfilmcehennemi.nl/`
2. JetFilmİzle — `https://jetfilmizle.now/`
3. DDizi — `https://www.ddizi.im/`
4. DiziBox — `https://www.dizibox.live/`
5. TrDiziİzle — `https://www.trdiziizle.tv/tr1/`
6. YabancıDizi — `https://yabancidizi.news/dizi-izle-hd`
7. DiziBal — `https://dizibal.com/`
8. FilmMakinesi — `https://filmmakinesi.to/`
9. DiziRella — `https://dizirella.net/`
10. HDFilmİzle Ink — `https://www.hdfilmizle.ink/`
11. DiziPal — `https://dizipal2216.com/`
12. DiziMom — `https://www.dizimom.work/`
13. DiziBol — `https://dizibol.com/`
14. FilmIzzle — `https://filmizzle.com/`
15. FullHDFilmİzlesene — `https://www.fullhdfilmizlesene.now/`
16. HDFilmİzle Vip — `https://www.hdfilmizle.vip/`
17. LiderFilmİzle — `https://liderfilmizle.com/`
18. SinemaGG — `https://www.sinema.gg/`
19. DiziFilmİzle — `https://dizifilmizle.to/`
20. FilmModu — `https://filmmodu.cc/`

Kaynaklar 13 `.cs3` paketi içinde dağıtılır. `OwnedSites` paketi ortak altyapı
kullanan yedi siteyi, `HDFilmIzle` paketi ise `.ink` ve `.vip` adreslerini ayrı
Cloudstream kaynakları olarak kaydeder.

## Yayınlama

1. Projeyi GitHub'da `cloudstream-repo` adlı repoya gönderin.
2. **Settings > Actions > General > Workflow permissions** altında **Read and write
   permissions** seçeneğini açın.
3. `main` dalına gönderim yapın. İş akışı 13 `.cs3` dosyasını ve
   `plugins.json` dosyasını `builds` dalında yayımlar.
4. `Oygy8b5F` Cutt.ly kodunun yukarıdaki ham `repo.json` adresine yönlendiğini doğrulayın.

Tam URL ile eklemek için:

```text
https://raw.githubusercontent.com/Ripplay/cloudstream-repo/main/repo.json
```

## Yerel derleme

Windows:

```powershell
.\gradlew.bat make makePluginsJson
```

Linux veya macOS:

```bash
./gradlew make makePluginsJson
```

Derlenen eklentiler her modülün `build/` klasörüne, liste ise `build/plugins.json`
dosyasına yazılır. Proje JDK 17 ve Android SDK 36 kullanır.

## Lisans ve atıf

Bu repo GNU GPL-3.0 lisansıyla dağıtılır. Uyarlanan sağlayıcılar
[`nikyokki/nik-cloudstream`](https://github.com/nikyokki/nik-cloudstream) projesinden
alınmış ve dosyalardaki özgün yazar atıfları korunmuştur. Yeni ortak sağlayıcı
altyapısı bu repo için eklenmiştir. Ayrıntılar için `LICENSE` dosyasına bakın.

Kaynak sitelerin HTML ve API yapıları değiştiğinde ilgili sağlayıcıların
güncellenmesi gerekir.
