-- Nettoyage des fiches joueurs "en minuscule" (Charles, 2026-09-23 : "tous les
-- joueurs ecrits en minuscule, tu mets en majuscule, tu recherches leurs prenoms
-- et leur nationalite sur le site de l'ATP, tu verifies les doublons").
--
-- Ces fiches "Nom P" avaient ete creees a la volee lors de saisies de tableaux
-- (nom en casse mixte, prenom reduit a une initiale), sans doublon MAJUSCULE
-- detecte par V14. Prenom complet et nationalite repris de la recherche ATP
-- (atptour.com) ; 5 joueurs introuvables sur l'ATP gardent leur initiale.
--
-- Fusions (resultats reaffectes a la fiche conservee, nom officiel ATP) :
--   1609 Friend J            -> 257 FRIEND JAY (ex HARA FRIEND JAY DYLAN)
--   1759 Goncalves Ceolin J  -> 863 GONCALVES CEOLIN JOAO VITOR (ex GONCALVES JOAO VICTOR)
--   1752 Von Der Schulenburg J -> 844 VON DER SCHULENBURG JEFFREY (ex SCHLENBURG, faute de frappe)
--   1725 Dellien Velasco M   -> 361 DELLIEN VELASCO MURKEL (ex DELLIEN MURKEL, demande de Charles)
-- Mallory : JAMAIQUE -> BERMUDES (nationalite ATP, confirme par Charles).
--
-- Contrairement a V12-V16, un conflit (les deux fiches inscrites au meme
-- tournoi) fait echouer la migration plutot que de supprimer une inscription :
-- une entry peut etre referencee par un match.
DO $$
DECLARE
    pairs CONSTANT INT[][] := ARRAY[
        ARRAY[257, 1609],
        ARRAY[863, 1759],
        ARRAY[844, 1752],
        ARRAY[361, 1725]
    ];
    pair INT[];
BEGIN
    FOREACH pair SLICE 1 IN ARRAY pairs LOOP
        IF EXISTS (
            SELECT 1 FROM entry a JOIN entry b ON a.tournament_id = b.tournament_id
            WHERE a.player_id = pair[1] AND b.player_id = pair[2]
        ) THEN
            RAISE EXCEPTION 'Fiches % et % inscrites au meme tournoi', pair[1], pair[2];
        END IF;
        UPDATE entry SET player_id = pair[1] WHERE player_id = pair[2];
        UPDATE player SET legacy_snapshot_points = COALESCE(legacy_snapshot_points,
            (SELECT legacy_snapshot_points FROM player WHERE id = pair[2]))
        WHERE id = pair[1];
        DELETE FROM player WHERE id = pair[2];
    END LOOP;
END $$;

UPDATE player SET last_name = 'ANTONI', first_name = 'RICHARD', nationality = 'ALLEMAGNE' WHERE id = 1737;
UPDATE player SET last_name = 'DE GABRIELE', first_name = 'ALEX', nationality = 'MALTE' WHERE id = 1720;
UPDATE player SET last_name = 'DEMUTH', first_name = 'OWEN', nationality = 'USA' WHERE id = 1708;
UPDATE player SET last_name = 'DIEVENEY', first_name = 'ELI', nationality = 'USA' WHERE id = 1733;
UPDATE player SET last_name = 'DJOSIC', first_name = 'NIKOLA', nationality = 'SUISSE' WHERE id = 1703;
UPDATE player SET last_name = 'DUARTE', first_name = 'RODRIGO', nationality = 'PORTUGAL' WHERE id = 1772;
UPDATE player SET last_name = 'DULLINGER', first_name = 'VINCENT', nationality = 'ALLEMAGNE' WHERE id = 1711;
UPDATE player SET last_name = 'ERLER', first_name = 'LANCELOT', nationality = 'FRANCE' WHERE id = 1658;
UPDATE player SET last_name = 'FABRE', first_name = 'JULES', nationality = 'FRANCE' WHERE id = 1578;
UPDATE player SET last_name = 'FARZAM', first_name = 'MATISSE', nationality = 'USA' WHERE id = 1686;
UPDATE player SET last_name = 'FERRE', first_name = 'CARLOS', nationality = 'ESPAGNE' WHERE id = 1702;
UPDATE player SET last_name = 'FILIP', first_name = 'JAKUB', nationality = 'TCHEQUIE' WHERE id = 1745;
UPDATE player SET last_name = 'FILIZ', first_name = 'SAMIM', nationality = 'TURQUIE' WHERE id = 1716;
UPDATE player SET last_name = 'FREITAS', first_name = 'JOSE', nationality = 'PORTUGAL' WHERE id = 1770;
UPDATE player SET last_name = 'FRIEND', first_name = 'JAY', nationality = 'JAPON' WHERE id = 257;  -- fiche conservee pour FRIEND JAY (ex-id 1609)
UPDATE player SET last_name = 'FRUNZA', first_name = 'LUCA', nationality = 'ROUMANIE' WHERE id = 1660;
UPDATE player SET last_name = 'FRUTOS ALONSO', first_name = 'ALVARO', nationality = 'PARAGUAY' WHERE id = 1597;
UPDATE player SET last_name = 'FRYDRYCH', first_name = 'VIKTOR', nationality = 'ANGLETERRE' WHERE id = 1749;
UPDATE player SET last_name = 'GAILLARD', first_name = 'TIMEO', nationality = 'SUISSE' WHERE id = 1754;
UPDATE player SET last_name = 'GALARRAGA', first_name = 'MARIO ANDRE', nationality = 'EQUATEUR' WHERE id = 1642;
UPDATE player SET last_name = 'GANCHEV', first_name = 'ALEX', nationality = 'BULGARIE' WHERE id = 1625;
UPDATE player SET last_name = 'GARBERO', first_name = 'FILIPPO FRANCESCO', nationality = 'ITALIE' WHERE id = 1761;
UPDATE player SET last_name = 'GELDOF', first_name = 'BENOIT', nationality = 'FRANCE' WHERE id = 1659;
UPDATE player SET last_name = 'GENOV', first_name = 'ANTHONY', nationality = 'BULGARIE' WHERE id = 1778;
UPDATE player SET last_name = 'GIESE', first_name = 'KELLY', nationality = 'USA' WHERE id = 1696;
UPDATE player SET last_name = 'GIL GARCIA', first_name = 'AARON', nationality = 'LUXEMBOURG' WHERE id = 1692;
UPDATE player SET last_name = 'GODOY', first_name = 'SEBASTIAN', nationality = 'USA' WHERE id = 1735;
UPDATE player SET last_name = 'GONCALVES CEOLIN', first_name = 'JOAO VITOR', nationality = 'BRESIL' WHERE id = 863;  -- fiche conservee pour GONCALVES CEOLIN JOAO VITOR (ex-id 1759)
UPDATE player SET last_name = 'GORE', first_name = 'NICHOLAS', nationality = 'JAMAIQUE' WHERE id = 1757;
UPDATE player SET last_name = 'GREVELIUS', first_name = 'ERIK', nationality = 'SUEDE' WHERE id = 1664;
UPDATE player SET last_name = 'GUNA', first_name = 'ROBERT', nationality = 'ROUMANIE' WHERE id = 1653;
UPDATE player SET last_name = 'GURENKO', first_name = 'VOLODYMYR', nationality = 'CANADA' WHERE id = 1694;
UPDATE player SET last_name = 'GUSIC WAN', first_name = 'BENJAMIN', nationality = 'ANGLETERRE' WHERE id = 1750;
UPDATE player SET last_name = 'HAUPT', first_name = 'HENRI', nationality = 'ALLEMAGNE' WHERE id = 1755;
UPDATE player SET last_name = 'HERNANDEZ AGUILA', first_name = 'ABEL', nationality = 'ESPAGNE' WHERE id = 1657;
UPDATE player SET last_name = 'HIDALGO AGUIRRE', first_name = 'CESAR ARIEL', nationality = 'EQUATEUR' WHERE id = 1647;
UPDATE player SET last_name = 'HIGNETT', first_name = 'LIAM', nationality = 'ANGLETERRE' WHERE id = 1674;
UPDATE player SET last_name = 'HORWOOD', first_name = 'JARED', nationality = 'CANADA' WHERE id = 1693;
UPDATE player SET last_name = 'HSU', first_name = 'JEFFREY CHUAN EN', nationality = 'TAIWAN' WHERE id = 1603;
UPDATE player SET last_name = 'HUENS', first_name = 'HAROLD', nationality = 'BELGIQUE' WHERE id = 1667;
UPDATE player SET last_name = 'HURTADO NOVOA', first_name = 'NICOLAS', nationality = 'COLOMBIE' WHERE id = 1666;
UPDATE player SET last_name = 'IEMMI', first_name = 'LEONARDO', nationality = 'ITALIE' WHERE id = 1684;
UPDATE player SET last_name = 'JECAN', first_name = 'ALEXANDRU', nationality = 'ROUMANIE' WHERE id = 1605;
UPDATE player SET last_name = 'JEFFERSON', first_name = 'HENRY', nationality = 'ANGLETERRE' WHERE id = 1586;
UPDATE player SET last_name = 'JIANG', first_name = 'FUMIN', nationality = 'CHINE' WHERE id = 1767;
UPDATE player SET last_name = 'KAMATH', first_name = 'MADHWIN', nationality = 'INDE' WHERE id = 1672;
UPDATE player SET last_name = 'KARMA', first_name = 'NOAH', nationality = 'SUISSE' WHERE id = 1580;
UPDATE player SET last_name = 'KIRCI', first_name = 'KORAY', nationality = 'TURQUIE' WHERE id = 1715;
UPDATE player SET last_name = 'KIROV', first_name = 'VIKTOR', nationality = 'BULGARIE' WHERE id = 1774;
UPDATE player SET last_name = 'KONG', first_name = 'WEIYI', nationality = 'CHINE' WHERE id = 1768;
UPDATE player SET last_name = 'KOZLOVSKY', first_name = 'MATYAS', nationality = 'TCHEQUIE' WHERE id = 1748;
UPDATE player SET last_name = 'KUCERA', first_name = 'JONAS', nationality = 'TCHEQUIE' WHERE id = 1714;
UPDATE player SET last_name = 'KUDERNATSCH', first_name = 'MORITZ', nationality = 'ALLEMAGNE' WHERE id = 1709;
UPDATE player SET last_name = 'LARWIG', first_name = 'NOEL', nationality = 'ALLEMAGNE' WHERE id = 1679;
UPDATE player SET last_name = 'LAWLOR', first_name = 'RHYS', nationality = 'ANGLETERRE' WHERE id = 1587;
UPDATE player SET last_name = 'LEEMAN', first_name = 'DYLAN', nationality = 'IRLANDE' WHERE id = 1596;
UPDATE player SET last_name = 'LEROUX', first_name = 'JULES', nationality = 'FRANCE' WHERE id = 1608;
UPDATE player SET last_name = 'LOGRIPPO', first_name = 'MATTIA', nationality = 'ITALIE' WHERE id = 1719;
UPDATE player SET last_name = 'LOPEZ ESCRIBANO', first_name = 'ALEJANDRO', nationality = 'ESPAGNE' WHERE id = 1699;
UPDATE player SET last_name = 'LORINCIK', first_name = 'MAX', nationality = 'SLOVAQUIE' WHERE id = 1581;
UPDATE player SET last_name = 'LORUSSO', first_name = 'LORENZO', nationality = 'ITALIE' WHERE id = 1691;
UPDATE player SET last_name = 'LUXA', first_name = 'TOMMY', nationality = 'TCHEQUIE' WHERE id = 1713;
UPDATE player SET last_name = 'MACIAS ELIZALDE', first_name = 'DARWIN ANDRES', nationality = 'EQUATEUR' WHERE id = 1648;
UPDATE player SET last_name = 'MACKINLAY', first_name = 'JAMES', nationality = 'ANGLETERRE' WHERE id = 1673;
UPDATE player SET last_name = 'MAJDANDZIC', first_name = 'OLIVER', nationality = 'ALLEMAGNE' WHERE id = 1676;
UPDATE player SET last_name = 'MAJDANDZIC', first_name = 'MARC', nationality = 'ALLEMAGNE' WHERE id = 1677;
UPDATE player SET last_name = 'MALLORY', first_name = 'RICHARD', nationality = 'BERMUDES' WHERE id = 1743;
UPDATE player SET last_name = 'MANUKYAN', first_name = 'VARDAN', nationality = 'RUSSIE' WHERE id = 1773;
UPDATE player SET last_name = 'MARES', first_name = 'VOJTECH', nationality = 'TCHEQUIE' WHERE id = 1747;
UPDATE player SET last_name = 'MARINESCU', first_name = 'MIHAI RAZVAN', nationality = 'ROUMANIE' WHERE id = 1652;
UPDATE player SET last_name = 'MARKOV', first_name = 'VIKTOR', nationality = 'BULGARIE' WHERE id = 1626;
UPDATE player SET last_name = 'MARTIN ESPINAR', first_name = 'ADAM', nationality = 'ESPAGNE' WHERE id = 1701;
UPDATE player SET last_name = 'MATIC', first_name = 'DOMINIK', nationality = 'ALLEMAGNE' WHERE id = 1756;
UPDATE player SET last_name = 'MATOS', first_name = 'RAFAEL', nationality = 'BRESIL' WHERE id = 1681;
UPDATE player SET last_name = 'MAZDRASHKI', first_name = 'ANAS', nationality = 'BULGARIE' WHERE id = 1627;
UPDATE player SET last_name = 'MAZZOLA', first_name = 'FILIPPO', nationality = 'ITALIE' WHERE id = 1582;
UPDATE player SET last_name = 'MBITHI', first_name = 'MWENDWA', nationality = 'USA' WHERE id = 1645;
UPDATE player SET last_name = 'MCGLOUGHLIN', first_name = 'JAMES', nationality = 'IRLANDE' WHERE id = 1595;
UPDATE player SET last_name = 'MERTGENS', first_name = 'NICK', nationality = 'ALLEMAGNE' WHERE id = 1722;
UPDATE player SET last_name = 'MOUILLERON SALVO', first_name = 'MANUEL', nationality = 'ARGENTINE' WHERE id = 1584;
UPDATE player SET last_name = 'MUNOZ', first_name = 'ENMANUEL', nationality = 'REPUBLIQUE DOMINICAINE' WHERE id = 1758;
UPDATE player SET last_name = 'NAYDENOV', first_name = 'YOAN', nationality = 'BULGARIE' WHERE id = 1726;
UPDATE player SET last_name = 'NITTMANN', first_name = 'YANNIC', nationality = 'ALLEMAGNE' WHERE id = 1710;
UPDATE player SET last_name = 'NORDQUIST', first_name = 'ARVID', nationality = 'SUEDE' WHERE id = 1760;
UPDATE player SET last_name = 'PAARDEKOOPER', first_name = 'STIJN', nationality = 'HOLLANDE' WHERE id = 1688;
UPDATE player SET last_name = 'PAMPANIN', first_name = 'PIETRO', nationality = 'ITALIE' WHERE id = 1690;
UPDATE player SET last_name = 'PARTAL', first_name = 'MATEI MARIUS', nationality = 'ROUMANIE' WHERE id = 1662;
UPDATE player SET last_name = 'PEREZ NAVARRO', first_name = 'PABLO', nationality = 'ESPAGNE' WHERE id = 1697;
UPDATE player SET last_name = 'PLESHIVTSEV', first_name = 'EGOR', nationality = 'RUSSIE' WHERE id = 1730;
UPDATE player SET last_name = 'PLUNKETT', first_name = 'CONNOR', nationality = 'USA' WHERE id = 1685;
UPDATE player SET last_name = 'PRASHANTH', first_name = 'VIJAY SUNDAR', nationality = 'INDE' WHERE id = 1661;
UPDATE player SET last_name = 'PUTTERGILL', first_name = 'CALUM', nationality = 'AUSTRALIE' WHERE id = 1732;
UPDATE player SET last_name = 'RAKHMATULLAYEV', first_name = 'DANIAL', nationality = 'KAZAKHSTAN' WHERE id = 1727;
UPDATE player SET last_name = 'RATIU', first_name = 'NIELS', nationality = 'BELGIQUE' WHERE id = 1668;
UPDATE player SET last_name = 'RIVADENEIRA GALLEGOS', first_name = 'FELIPE', nationality = 'EQUATEUR' WHERE id = 1643;
UPDATE player SET last_name = 'ROCHE', first_name = 'MATIS', nationality = 'FRANCE' WHERE id = 1781;
UPDATE player SET last_name = 'ROUX', first_name = 'ELOI', nationality = 'CANADA' WHERE id = 1695;
UPDATE player SET last_name = 'RUIZ', first_name = 'JORGE', nationality = 'EQUATEUR' WHERE id = 1650;
UPDATE player SET last_name = 'SADZIK', first_name = 'JAN', nationality = 'POLOGNE' WHERE id = 1592;
UPDATE player SET last_name = 'SARITAS', first_name = 'GOKBERK', nationality = 'TURQUIE' WHERE id = 1717;
UPDATE player SET last_name = 'SEGHETTI', first_name = 'SAMUELE', nationality = 'ITALIE' WHERE id = 1613;
UPDATE player SET last_name = 'SEIDMAN', first_name = 'LEV', nationality = 'USA' WHERE id = 1741;
UPDATE player SET last_name = 'SEIFERT', first_name = 'STEFAN', nationality = 'ALLEMAGNE' WHERE id = 1680;
UPDATE player SET last_name = 'SENN', first_name = 'NICOLA', nationality = 'SUISSE' WHERE id = 1704;
UPDATE player SET last_name = 'SESKO', first_name = 'ZIGA', nationality = 'SLOVENIE' WHERE id = 1763;
UPDATE player SET last_name = 'SHANDAROV', first_name = 'RADOSLAV', nationality = 'BULGARIE' WHERE id = 1779;
UPDATE player SET last_name = 'SHIKOV', first_name = 'KALOYAN', nationality = 'BULGARIE' WHERE id = 1777;
UPDATE player SET last_name = 'SIANCHA BORLENGHI', first_name = 'AGUSTIN', nationality = 'ESPAGNE' WHERE id = 1700;
UPDATE player SET last_name = 'SINESCU', first_name = 'JAN', nationality = 'ROUMANIE' WHERE id = 1607;
UPDATE player SET last_name = 'SPASOV', first_name = 'DANIEL', nationality = 'BULGARIE' WHERE id = 1776;
UPDATE player SET last_name = 'ST-HILAIRE', first_name = 'MAXIME', nationality = 'CANADA' WHERE id = 1739;
UPDATE player SET last_name = 'STEPHENSON', first_name = 'ELI', nationality = 'USA' WHERE id = 1724;
UPDATE player SET last_name = 'SZYMKOWIAK', first_name = 'KACPER', nationality = 'POLOGNE' WHERE id = 1682;
UPDATE player SET last_name = 'TANG', first_name = 'SHENG', nationality = 'CHINE' WHERE id = 1766;
UPDATE player SET last_name = 'TAZABEKOV', first_name = 'DANIEL', nationality = 'KAZAKHSTAN' WHERE id = 1731;
UPDATE player SET last_name = 'THEATE', first_name = 'PAUL', nationality = 'FRANCE' WHERE id = 1601;
UPDATE player SET last_name = 'THIES', first_name = 'CHRISTOPHER', nationality = 'ALLEMAGNE' WHERE id = 1712;
UPDATE player SET last_name = 'TIXHON', first_name = 'DAVID', nationality = 'BELGIQUE' WHERE id = 1669;
UPDATE player SET last_name = 'TOMBOLINI', first_name = 'LEONARDO', nationality = 'ITALIE' WHERE id = 1588;
UPDATE player SET last_name = 'TRACY', first_name = 'JJ', nationality = 'USA' WHERE id = 1706;
UPDATE player SET last_name = 'UZHYLOVSKYI', first_name = 'VOLODYMYR', nationality = 'UKRAINE' WHERE id = 1600;
UPDATE player SET last_name = 'VALDOLEIROS', first_name = 'GUILHERME', nationality = 'PORTUGAL' WHERE id = 1771;
UPDATE player SET last_name = 'VAN SAMBEEK', first_name = 'FONS', nationality = 'HOLLANDE' WHERE id = 1687;
UPDATE player SET last_name = 'VANDERMEERSCH', first_name = 'CYRIL', nationality = 'FRANCE' WHERE id = 1698;
UPDATE player SET last_name = 'VARBANCIU', first_name = 'MATEI', nationality = 'ROUMANIE' WHERE id = 1655;
UPDATE player SET last_name = 'VELIZ', first_name = 'ANGEL', nationality = 'EQUATEUR' WHERE id = 1635;
UPDATE player SET last_name = 'VERVOORT', first_name = 'MARK', nationality = 'HOLLANDE' WHERE id = 1599;
UPDATE player SET last_name = 'VILLOSLADA', first_name = 'EDOUARD', nationality = 'FRANCE' WHERE id = 1671;
UPDATE player SET last_name = 'VON DER SCHULENBURG', first_name = 'JEFFREY', nationality = 'SUISSE' WHERE id = 844;  -- fiche conservee pour VON DER SCHULENBURG JEFFREY (ex-id 1752)
UPDATE player SET last_name = 'VRTILKA', first_name = 'JAKUB', nationality = 'TCHEQUIE' WHERE id = 1746;
UPDATE player SET last_name = 'WEIR', first_name = 'PHOENIX', nationality = 'ANGLETERRE' WHERE id = 1675;
UPDATE player SET last_name = 'WEISSMANN', first_name = 'YANNIK', nationality = 'ALLEMAGNE' WHERE id = 1723;
UPDATE player SET last_name = 'WILLWERTH', first_name = 'BENJAMIN', nationality = 'USA' WHERE id = 1594;
UPDATE player SET last_name = 'YERDILDA', first_name = 'YERASSYL', nationality = 'KAZAKHSTAN' WHERE id = 1729;
UPDATE player SET last_name = 'ZANNONI', first_name = 'SAMUEL', nationality = 'ITALIE' WHERE id = 1683;
UPDATE player SET last_name = 'LE MEUR', first_name = 'R' WHERE id = 1604;
UPDATE player SET last_name = 'LU', first_name = 'H' WHERE id = 1769;
UPDATE player SET last_name = 'QI', first_name = 'H' WHERE id = 1765;
UPDATE player SET last_name = 'MARC', first_name = 'L' WHERE id = 1663;
UPDATE player SET last_name = 'MAYHEW', first_name = 'I' WHERE id = 1611;
UPDATE player SET last_name = 'DELLIEN VELASCO', first_name = 'MURKEL' WHERE id = 361;
