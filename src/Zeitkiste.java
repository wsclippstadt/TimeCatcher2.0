import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Properties;

import javax.swing.JOptionPane;

public class Zeitkiste {
	
	public static Gpio gpio;
	public static Gui gui;
	public static File fileMan;
	public static File fileAuto;
	public static Display display;
	private static Database database;
	private static Startliste startliste;
	private static Properties props;
	private static String standort;
	private static DecimalFormat df = new DecimalFormat("000");;
	private static int lauf;
	private int aktuellerIndex = 0;
	private static int startnummer;
	private long letzteAutoZeit;
	private static long letzteManZeit;
	private static boolean lsScharf = false;
	private static boolean manZeitGenommen = false;
	private static boolean autoZeitGenommen = false;
	private static String disLeft;
	private static String disRight;
	private String zeileEins = "";
	private String zeileZwei = "";
	private String zeileDrei = "";
	private String zeileVier = "";
	
	public static void main(String[] args) throws IOException {
		props = new Properties();
		FileInputStream in = new FileInputStream("zeitkiste.ini");
		props.load(in);
		standort = props.getProperty("standort"); //Einstellung des Standortes
		lauf = Integer.parseInt(props.getProperty("lauf")); //Einstellung des Laufes
		gui = new Gui();
		startliste = new Startliste();
		database = new Database();
		gpio = new Gpio();
		fileMan = new File(standort + "_" + lauf + "_man");
		fileAuto = new File(standort + "_" + lauf + "_auto");
		display = new Display();
		System.out.println("Zeitkiste " + getStandort() + ", " + getLauf() + ". Lauf ist einsatzbereit!");
	}
	public static void einstellungenAendern(String pStandort, int pLauf) throws IOException {
		FileOutputStream out = new FileOutputStream("standort.ini");
		if (pStandort != null) {
			props.setProperty("standort", pStandort);
			props.store(out, null);
		} else if (pLauf != 0) {
			props.setProperty("lauf", Integer.toString(pLauf));
			props.store(out, null);	
		}
	}
	public void setLsScharf() {
		if (autoZeitGenommen == false) {
			if (lsScharf == false && manZeitGenommen == false) {
				this.ersteZeileAktualisieren("       ", "*******");
				lsScharf = true;
				System.out.println("Lichtschranke wurde scharfgestellt f�r Startnummer: " + startnummer);
			} else if (lsScharf == false && manZeitGenommen == true) {
				this.ersteZeileAktualisieren(disZeit(letzteManZeit), "*******");
				lsScharf = true;
				System.out.println("Lichtschranke wurde scharfgestellt f�r Startnummer: " + startnummer);
			}
		}
	}
	
	public void lsAusgeloest() throws IOException {
		if (lsScharf == true) {
			letzteAutoZeit = System.currentTimeMillis(); //Zeit in Variable sichern
			fileAuto.write(startnummer, letzteManZeit); //Zeit in Datei sichern
			lsScharf = false; //Lcihtschranke deaktivieren um Doppelausl�sung zu vermeiden
			database.writeAuto();
			autoZeitGenommen = true;
			if (manZeitGenommen == true) {
				this.ersteZeileAktualisieren(disZeit(letzteManZeit), disZeit(letzteAutoZeit));
			} else if (manZeitGenommen == false) {
				this.ersteZeileAktualisieren("       ", disZeit(letzteAutoZeit));
			}
			System.out.println("Lichtschranke wurde erwartet ausgel�st:");
			System.out.println(startnummer + " : " + letzteAutoZeit);
		} else {
			letzteAutoZeit = System.currentTimeMillis();
			System.out.println("Lichtschranke wurde unerwartet ausgel�st:");
			System.out.println(startnummer + " : " + letzteAutoZeit);
		}
	}
	
	public void manAusgeloest() throws IOException {
		if (manZeitGenommen == false) {
			letzteManZeit = System.currentTimeMillis(); //Zeit in Variable sichern
			fileMan.write(startnummer, letzteManZeit); //Zeit in Datei sichern
			database.writeMan();
			manZeitGenommen = true;
			if (autoZeitGenommen == true) {
				this.ersteZeileAktualisieren(disZeit(letzteManZeit), disZeit(letzteAutoZeit));
			} else if (autoZeitGenommen == false && lsScharf == true) {
				this.ersteZeileAktualisieren(disZeit(letzteManZeit), "*******");
			} else if (autoZeitGenommen == false && lsScharf == false) {
				this.ersteZeileAktualisieren(disZeit(letzteManZeit), "       ");
			}
			System.out.println("Manuelle Zeitnahme wurde erwartet ausgel�st:");
			System.out.println(startnummer + " : " + letzteManZeit);
		} else {
			System.out.println("Zweite Manuelle Zeit genommen f�r Startnummer " + startnummer + ", " + System.currentTimeMillis());
		}
	}
	
	public void pressedUp() {
		aktuellerIndex++;
		if (aktuellerIndex > startliste.getStnrListe().size()-1){
			aktuellerIndex=0;
		}
		startnummer = startliste.getStnrListe().get(aktuellerIndex);
		lsScharf = false;
		if (manZeitGenommen == true || autoZeitGenommen == true) {
			this.displayAktualisieren();
		} else {
			this.ersteZeileAktualisieren("", "");
		}
		manZeitGenommen = false;
		autoZeitGenommen = false;
		letzteManZeit = 0;
		letzteAutoZeit = 0;
	}
	
	public void pressedDown() {
		aktuellerIndex--;
		if (aktuellerIndex < 0) {
			aktuellerIndex=startliste.getStnrListe().size()-1;
		}
		startnummer = startliste.getStnrListe().get(aktuellerIndex);
		lsScharf = false;
		if (manZeitGenommen == true || autoZeitGenommen == true) {
			this.displayAktualisieren();
		} else {
			this.ersteZeileAktualisieren("", "");
		}
		manZeitGenommen = false;
		autoZeitGenommen = false;
		letzteManZeit = 0;
		letzteAutoZeit = 0;
		
	}
	
	public void zuStnrSpringen(int pStartnummer) {
		if (startliste.getStnrListe().contains(pStartnummer)){
			aktuellerIndex = startliste.getStnrListe().indexOf(pStartnummer);
			startnummer = pStartnummer;
			if (manZeitGenommen == true || autoZeitGenommen == true) {
				this.displayAktualisieren();
			} else {
				this.ersteZeileAktualisieren("       ", "       ");
			}
			manZeitGenommen = false;
			autoZeitGenommen = false;
			letzteManZeit = 0;
			letzteAutoZeit = 0;
		} else {
			JOptionPane.showMessageDialog(null,"Startnumer in Startliste nicht gefunden","Fehler",JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public void ersteZeileAktualisieren(String pDisLeft, String pDisRight) {
		zeileEins = df.format(startnummer) + ": " + pDisLeft + " " + pDisRight;
		gui.virtDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);
		display.phyDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);		
	}
	
	public void displayAktualisieren() {
		zeileVier = zeileDrei;
		zeileDrei = zeileZwei;
		zeileZwei = zeileEins;
		this.ersteZeileAktualisieren("       ", "        ");
		gui.virtDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);
		display.phyDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);
	}
	
	public String disZeit(long pZeit) {
		if (pZeit != 0) { //Kuerzt die Zeit runter
			String pString = Long.toString(pZeit);
			pString = pString.substring(pString.length()-7, pString.length());
			return pString;
		} else {
			return "       ";
		}
	}
	public static String getStandort() {
		return standort;
	}
	public static int getLauf() {
		return lauf;
	}
	public void setStandort(String pStandort) throws IOException {
		standort = pStandort;
		FileOutputStream out = new FileOutputStream("zeitkiste.ini");
			props.setProperty("standort", pStandort);
			props.store(out, null);
	}
	public void setLauf(int pLauf) throws IOException{
		lauf = pLauf;
		FileOutputStream out = new FileOutputStream("zeitkiste.ini");
			props.setProperty("lauf", Integer.toString(pLauf));
			props.store(out, null);	
	}
	public void warnungAusgeben(String pWarnung) {
		zeileDrei = "Warnung vom Turm:";
		zeileVier = pWarnung;
		gui.virtDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);
		display.phyDisplayAktualisieren(zeileEins, zeileZwei, zeileDrei, zeileVier);
	}
	

}