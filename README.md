Kernsoftware
============

Die Kernsoftware des Datenverteilers in Form eines Maven-Projekts. Der Zweck
dieses Projekts ist es, die Softwareinheiten (SWE) der Kernsoftware als
Maven-Artefakte zur Verfügung zu stellen und so als Maven-Abhängigkeit nutzbar
zu machen.

Das Projekt beinhaltet die SWEs, die vom NERZ e.V. als Paket *Kernsoftware* zum
Download bereit gestellt werden.

Die Projektdokumentation befindet sich unter
http://falkoschumann.github.io/datenverteiler-kernsoftware/

Das Maven Repository http://projekte.muspellheim.de/repository/ enthält alle
mit diesem Projekt erzeugten Artefakte der Kernsofware: Binary JARs, Source JARs
und JavaDoc JARs.


Der *master* Branch
-------------------

Der *master* Branch enthält die Kernsoftware in Version 3.5.0 vom 15.04.2012.


Der *develop* Branch
--------------------

Der *develop* Branch umfasst gegenüber dem *master* Branch folgende Änderungen:

- *de.bsvrz.dav.daf* wurde auf Version 3.5.5 vom 13.15.2012 aktualisiert.


Hinweise zur Maven-Konfiguration
--------------------------------

Die SWEs der Kernsoftware sind als Unterprojekte angelegt. Im Root-Projekt sind
alle gemeinsamen Einstellungen konfiguriert, einschließlich der Sektionen für
*build* und *reporting*.

In den Unterprojekten der SWEs sind nur vom Root-Projekt abweichende
Einstellungen konfiguriert. Insbesondere bei Reports ist im Root-Projekt der
aggregierte Report aktiviert und im Unterprojekt wieder deaktiviert.


---

Dieses Projekt ist nicht Teil des NERZ e.V. Die offizielle Software sowie
weitere Informationen zur bundeseinheitlichen Software für
Verkehrsrechnerzentralen (BSVRZ) finden Sie unter http://www.nerz-ev.de.