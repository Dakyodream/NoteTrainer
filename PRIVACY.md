# Politique de confidentialité — NoteTrainer

**Date de dernière mise à jour : octobre 2026**

NoteTrainer est un projet open source (licence MIT), gratuit, sans publicité
et **100 % hors-ligne**.

## Données collectées

**Aucune.** NoteTrainer ne collecte, ne transmet et ne vend aucune donnée
personnelle. L'application ne demande **aucune permission** Android (ni réseau,
ni stockage, ni contacts, ni micro).

## Données stockées localement

L'application stocke sur votre appareil, dans son stockage privé
(inaccessible aux autres applications) :

- `stats.json` : vos scores de jeu (mode, difficulté, score, date)
- Vos préférences (thème, notation, clé) en mémoire de session

Ces fichiers ne sont **jamais** transmis où que ce soit. Ils sont exclus des
sauvegardes Android (adb, cloud, transfert d'appareil) via
`dataExtractionRules`.

## Vos droits (RGPD)

Conformément au RGPD, vous disposez sur **vos propres données** (locales) de :

- **Portabilité** : bouton « Exporter mes stats » dans la page Statistiques
  (génère un fichier JSON que vous choisissez de partager ou non)
- **Effacement** : bouton « Effacer mes données » dans la page Statistiques
  (suppression définitive et immédiate de `stats.json`)

Désinstaller l'application supprime également toutes les données.

## Contact

Uniquement via le dépôt GitHub du projet. Aucune donnée ne transite par un
serveur que nous contrôlons — il n'y a donc rien à demander, rien à rectifier
auprès d'un tiers : tout est sur votre appareil.

## Code source et vérification

La promesse ci-dessus est vérifiable dans le code :
[NoteTrainer sur GitHub](https://github.com/Dakyodream/NoteTrainer) —
le manifeste ne déclare aucune permission, aucune dépendance réseau n'est
présente, et `StatsStore` n'écrit que dans `filesDir`.
