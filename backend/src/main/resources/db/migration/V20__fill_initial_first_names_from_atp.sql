-- Suite de V19 : prenoms reduits a une initiale (ou vides) completes depuis la
-- recherche ATP (Charles, 2026-09-23 : "il faut que tu remplisses egalement les
-- prenoms ne portant qu'une seule lettre").
--
-- Fusions (resultats reaffectes a la fiche conservee, nom officiel ATP) :
--   1439 DEV S                  -> 3   DEV S D PRAJWAL (ex DEV PRAJWAL)
--   1522 LONGWE-SMIT T          -> 972 LONGWE-SMIT THANDO
--   1420 MONTES-DE LA TORRE I   -> 266 MONTES-DE LA TORRE INAKI
--   1557 REJCHTMAN VINCIGUERRA W -> 846 REJCHTMAN VINCIGUERRA WILLIAM (ex REJCTHMAN, SUISSE -> SUEDE)
--   1408 REMONDY PAGOTTO V      -> 773 REMONDY PAGOTTO VICTOR HUGO (ex PAGOTTTO)
--   1152 SAKAMOTO               -> 156 SAKAMOTO REI
-- Choix confirmes par Charles : KIM D = KIM DONGJAE ; nationalites ATP pour
-- Nawa, Petit, Schtulmann Gasca (absentes), Gundacker, Strydom, Kukasian.
-- Non trouves ou ambigus sur l'ATP (initiale conservee) : HU H, LU H, QI H,
-- ZHAO Z, LEE D, JOVANIVSKI, LARA SALMERON D, MOXON W, RODRIGUES LONGOBARDI P,
-- SALUN-OUILLEMON, SCOTT A, SIM S, TE R, THOMSON KEI, VAN SCHALKWYK C,
-- BRUCE SMITH, MARC L, MAYHEW I, LE MEUR R.
--
-- Comme V19, un conflit (deux fiches inscrites au meme tournoi) fait echouer
-- la migration plutot que de supprimer une inscription.
DO $$
DECLARE
    pairs CONSTANT INT[][] := ARRAY[
        ARRAY[3, 1439],
        ARRAY[972, 1522],
        ARRAY[266, 1420],
        ARRAY[846, 1557],
        ARRAY[773, 1408],
        ARRAY[156, 1152]
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

UPDATE player SET last_name = 'CHEKHOV', first_name = 'ANTON' WHERE id = 1457;
UPDATE player SET last_name = 'DEV', first_name = 'S D PRAJWAL' WHERE id = 3;  -- fiche conservee (ex-id 1439)
UPDATE player SET last_name = 'FITA JUAN', first_name = 'SERGI' WHERE id = 1511;
UPDATE player SET last_name = 'FIX', first_name = 'DAVID' WHERE id = 1552;
UPDATE player SET last_name = 'FORNACI', first_name = 'DIEGO' WHERE id = 1554;
UPDATE player SET last_name = 'FURNESS', first_name = 'EVAN' WHERE id = 1177;
UPDATE player SET last_name = 'GALLAGHER', first_name = 'GORDON' WHERE id = 1448;
UPDATE player SET last_name = 'GAVA', first_name = 'PIETRO' WHERE id = 1410;
UPDATE player SET last_name = 'GOYAL', first_name = 'MEDHIR' WHERE id = 1486;
UPDATE player SET last_name = 'GREGORIOU', first_name = 'GEORGE' WHERE id = 1489;
UPDATE player SET last_name = 'GSCHWENDTNER', first_name = 'JEREMY' WHERE id = 1545;
UPDATE player SET last_name = 'HALAHIJA', first_name = 'NATHAN ANDRE' WHERE id = 1537;
UPDATE player SET last_name = 'HANCE', first_name = 'KEATON' WHERE id = 1435;
UPDATE player SET last_name = 'HANDS', first_name = 'TOM' WHERE id = 1460;
UPDATE player SET last_name = 'HANS', first_name = 'KABIR' WHERE id = 1502;
UPDATE player SET last_name = 'HAUSBERGER', first_name = 'GREGOR' WHERE id = 1470;
UPDATE player SET last_name = 'HERMASSI', first_name = 'RAYEN' WHERE id = 1506;
UPDATE player SET last_name = 'HILDERBRAND', first_name = 'TREY' WHERE id = 1472;
UPDATE player SET last_name = 'HODKIN', first_name = 'SEAN' WHERE id = 1548;
UPDATE player SET last_name = 'HORNUNG', first_name = 'GABOR' WHERE id = 1436;
UPDATE player SET last_name = 'HYUN', first_name = 'JUN HA' WHERE id = 1444;
UPDATE player SET last_name = 'IANNONI', first_name = 'MARCCO' WHERE id = 1411;
UPDATE player SET last_name = 'IBRAIMI', first_name = 'YMERALI' WHERE id = 1171;
UPDATE player SET last_name = 'ILKEL', first_name = 'CEM' WHERE id = 1517;
UPDATE player SET last_name = 'IZQUIERDO LUQUE', first_name = 'RAFAEL' WHERE id = 1422;
UPDATE player SET last_name = 'JUSZCZAK', first_name = 'PAWEL' WHERE id = 1481;
UPDATE player SET last_name = 'KADANGAH-KILI', first_name = 'JEAN-PAUL' WHERE id = 1442;
UPDATE player SET last_name = 'KADHE', first_name = 'ARJUN' WHERE id = 1532;
UPDATE player SET last_name = 'KALIYANDA POONACHA', first_name = 'NIKI' WHERE id = 1497;
UPDATE player SET last_name = 'KANG', first_name = 'KU KEON' WHERE id = 1424;
UPDATE player SET last_name = 'KESHARWANI', first_name = 'MAAN' WHERE id = 1496;
UPDATE player SET last_name = 'KITTAY', first_name = 'BENJAMIN' WHERE id = 1407;
UPDATE player SET last_name = 'KLIMAS', first_name = 'JAN' WHERE id = 1480;
UPDATE player SET last_name = 'KOFFI', first_name = 'MOYE LUCAS NADAL' WHERE id = 1443;
UPDATE player SET last_name = 'KOTHAPALLI', first_name = 'GANDHARV GOURAV' WHERE id = 1498;
UPDATE player SET last_name = 'KOZLOV', first_name = 'BORIS' WHERE id = 1449;
UPDATE player SET last_name = 'KUSY', first_name = 'JAKUB' WHERE id = 1479;
UPDATE player SET last_name = 'LANIK', first_name = 'TOMAS' WHERE id = 1538;
UPDATE player SET last_name = 'LONGWE-SMIT', first_name = 'THANDO' WHERE id = 972;  -- fiche conservee (ex-id 1522)
UPDATE player SET last_name = 'LUKASHOV', first_name = 'RODION' WHERE id = 1458;
UPDATE player SET last_name = 'MACEJ', first_name = 'DOMINIK' WHERE id = 1539;
UPDATE player SET last_name = 'MAHESH KUMAR', first_name = 'VISHAL VASUDEV' WHERE id = 1501;
UPDATE player SET last_name = 'MARQUES', first_name = 'GONCALO' WHERE id = 1505;
UPDATE player SET last_name = 'MATUSZEWSKI', first_name = 'PIOTR' WHERE id = 1531;
UPDATE player SET last_name = 'MEHROTRA', first_name = 'ARJUN' WHERE id = 1514;
UPDATE player SET last_name = 'MENESES PERNY', first_name = 'ASIER' WHERE id = 1418;
UPDATE player SET last_name = 'MICHALIK', first_name = 'RADOVAN' WHERE id = 1535;
UPDATE player SET last_name = 'MINGZHOU', first_name = 'ZHOU' WHERE id = 1438;
UPDATE player SET last_name = 'MINTZ', first_name = 'ZACHARY' WHERE id = 1488;
UPDATE player SET last_name = 'MOLLER', first_name = 'ELMER' WHERE id = 1430;
UPDATE player SET last_name = 'MONTEIRO', first_name = 'SALVADOR' WHERE id = 1504;
UPDATE player SET last_name = 'MONTES-DE LA TORRE', first_name = 'INAKI' WHERE id = 266;  -- fiche conservee (ex-id 1420)
UPDATE player SET last_name = 'NEDUNCHEZHIYAN', first_name = 'JEEVAN' WHERE id = 1500;
UPDATE player SET last_name = 'NGWENYA', first_name = 'SIMPHIWE' WHERE id = 1523;
UPDATE player SET last_name = 'NSAIRI', first_name = 'SKANDER' WHERE id = 1508;
UPDATE player SET last_name = 'NURLANULY', first_name = 'ZANGAR' WHERE id = 1450;
UPDATE player SET last_name = 'OMARKHANOV', first_name = 'AMIR' WHERE id = 1451;
UPDATE player SET last_name = 'OZDEMIR', first_name = 'S MERT' WHERE id = 1459;
UPDATE player SET last_name = 'PAN', first_name = 'WEIWEN' WHERE id = 1441;
UPDATE player SET last_name = 'PANDZOU EKOUME', first_name = 'CHEIK' WHERE id = 1484;
UPDATE player SET last_name = 'PAOLINI', first_name = 'ANDREA' WHERE id = 1491;
UPDATE player SET last_name = 'PASTORINI', first_name = 'ALESSANDRO' WHERE id = 1446;
UPDATE player SET last_name = 'PINTER', first_name = 'PIET LUIS' WHERE id = 1469;
UPDATE player SET last_name = 'PINTO', first_name = 'LUCCA' WHERE id = 1494;
UPDATE player SET last_name = 'POSTOLKA', first_name = 'ROMAN' WHERE id = 1478;
UPDATE player SET last_name = 'PUCINELLI DE ALMEIDA', first_name = 'RAPHAEL' WHERE id = 1495;
UPDATE player SET last_name = 'RALLIN', first_name = 'SALVADOR' WHERE id = 1433;
UPDATE player SET last_name = 'REJCHTMAN VINCIGUERRA', first_name = 'WILLIAM', nationality = 'SUEDE' WHERE id = 846;  -- fiche conservee (ex-id 1557)
UPDATE player SET last_name = 'REMONDY PAGOTTO', first_name = 'VICTOR HUGO' WHERE id = 773;  -- fiche conservee (ex-id 1408)
UPDATE player SET last_name = 'RIVERO', first_name = 'MATIAS' WHERE id = 1431;
UPDATE player SET last_name = 'RODDICK', first_name = 'JERRY' WHERE id = 1542;
UPDATE player SET last_name = 'ROLLIN', first_name = 'NELL' WHERE id = 1159;
UPDATE player SET last_name = 'ROOTHMAN', first_name = 'CARL' WHERE id = 1551;
UPDATE player SET last_name = 'ROSENKRANZ KOENIG', first_name = 'TIMO' WHERE id = 1475;
UPDATE player SET last_name = 'ROSSOLINO', first_name = 'SIMONE' WHERE id = 1447;
UPDATE player SET last_name = 'SALTON', first_name = 'DYLAN' WHERE id = 1526;
UPDATE player SET last_name = 'SARRAN', first_name = 'PRAKAASH' WHERE id = 1499;
UPDATE player SET last_name = 'SAULENKO', first_name = 'TIMUR' WHERE id = 1482;
UPDATE player SET last_name = 'SENTHIL KUMAR', first_name = 'RETHIN PRANAV' WHERE id = 1515;
UPDATE player SET last_name = 'SHIN', first_name = 'WOOBIN' WHERE id = 1425;
UPDATE player SET last_name = 'SHIN', first_name = 'MAXIM' WHERE id = 1454;
UPDATE player SET last_name = 'SNITARI', first_name = 'ILYA' WHERE id = 1528;
UPDATE player SET last_name = 'SPADOLA', first_name = 'ALESSANDRO' WHERE id = 1445;
UPDATE player SET last_name = 'SUAREZ', first_name = 'LEONARDO' WHERE id = 1434;
UPDATE player SET last_name = 'SUKSUMRARN', first_name = 'THANTUB' WHERE id = 1167;
UPDATE player SET last_name = 'SUN', first_name = 'QIAN' WHERE id = 1414;
UPDATE player SET last_name = 'SUN', first_name = 'ZHENGYU' WHERE id = 1466;
UPDATE player SET last_name = 'TEUNISSEN', first_name = 'LAURENCE' WHERE id = 1521;
UPDATE player SET last_name = 'TIAN', first_name = 'YUXIANG' WHERE id = 1463;
UPDATE player SET last_name = 'TIUKAEV', first_name = 'RUSLAN' WHERE id = 1456;
UPDATE player SET last_name = 'TONEJC', first_name = 'VITO' WHERE id = 1513;
UPDATE player SET last_name = 'TREBUKHIN', first_name = 'ARSENIY' WHERE id = 1455;
UPDATE player SET last_name = 'TULEPBERGENOV', first_name = 'DIAS' WHERE id = 1453;
UPDATE player SET last_name = 'UJVARY', first_name = 'MATTHIAS' WHERE id = 1467;
UPDATE player SET last_name = 'VASA', first_name = 'IIRO' WHERE id = 1483;
UPDATE player SET last_name = 'VICENTE DE ARAUJO', first_name = 'RAI' WHERE id = 1493;
UPDATE player SET last_name = 'VILMAURE', first_name = 'GAUDERIC' WHERE id = 1485;
UPDATE player SET last_name = 'VITHOONTIEN', first_name = 'LEO' WHERE id = 1437;
UPDATE player SET last_name = 'VOISIN', first_name = 'EMILIEN' WHERE id = 1158;
UPDATE player SET last_name = 'VUKADIN', first_name = 'NOA' WHERE id = 1512;
UPDATE player SET last_name = 'WAGNER', first_name = 'ALEXANDER' WHERE id = 1473;
UPDATE player SET last_name = 'WALDNER', first_name = 'NIKLAS' WHERE id = 1471;
UPDATE player SET last_name = 'WEI', first_name = 'JIANGNAN' WHERE id = 1464;
UPDATE player SET last_name = 'ZGIROVSKY', first_name = 'ALEXANDER' WHERE id = 1520;
UPDATE player SET last_name = 'ZHANG', first_name = 'LINGHAO' WHERE id = 1440;
UPDATE player SET last_name = 'KIM', first_name = 'DONGJAE' WHERE id = 1428;
UPDATE player SET last_name = 'NAWA', first_name = 'MARK', nationality = 'BOTSWANA' WHERE id = 1525;
UPDATE player SET last_name = 'PETIT', first_name = 'LENNY', nationality = 'MONACO' WHERE id = 1487;
UPDATE player SET last_name = 'SCHTULMANN GASCA', first_name = 'MAURICIO', nationality = 'MEXIQUE' WHERE id = 1405;
UPDATE player SET last_name = 'GUNDACKER', first_name = 'JONAS', nationality = 'AUTRICHE' WHERE id = 1476;
UPDATE player SET last_name = 'STRYDOM', first_name = 'JEFFREY', nationality = 'AFRIQUE SUD' WHERE id = 1550;
UPDATE player SET last_name = 'KUKASIAN', first_name = 'ARTUR', nationality = 'RUSSIE' WHERE id = 1174;
