# OneOfAKind

Plugin pod Paper 26.3. Na serwerze da się zrobić tylko jedną sztukę maca i jedną sztukę każdego
przedmiotu z netherytu (miecz, kilof, siekiera, łopata, motyka, włócznia, hełm, napierśnik, getry, buty).
Każdy typ ma własny limit: zrobienie netherytowego miecza nie blokuje netherytowego hełmu.

## Budowanie

Potrzebujesz JDK 25 i Maven 3.9+ (`java -version` musi pokazać 25).

    mvn clean package

Gotowy plugin: `target/OneOfAKind-1.0.0.jar`. Wrzuć go do `plugins/` na serwerze i zrestartuj.

Jeśli Maven nie znajdzie `paper-api`, sprawdź na https://docs.papermc.io dokładny format wersji dla 26.3
i popraw `<version>` w pom.xml.

## Komenda

    /jedyny list            pokazuje, co już zostało zrobione
    /jedyny reset <ITEM>    zdejmuje limit z jednego przedmiotu, np. /jedyny reset MACE
    /jedyny reset all       zdejmuje wszystkie limity

Wymaga uprawnienia `jedyny.admin` (domyślnie OP). Stan zapisuje się w `plugins/OneOfAKind/data.yml`.
