public class TestOiseau{
    public static void main(String[] args){
        Oiseau oiseau=new Oiseau(100.0, 250.0);
        oiseau.afficherEtat();

        oiseau.flap();
        oiseau.mettreAJourGravite();
        oiseau.gagnerPieces(5);
        oiseau.afficherEtat();

        oiseau.perdreVie();
        System.out.println("Vies actuelles via getter : " + oiseau.getVies());
    }
}