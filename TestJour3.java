public class TestJour3{
    public static void main(String[] args){
        Oiseau oiseau=new Oiseau(100.0,220.0);
        Tuyau tuyau=new Tuyau(400.0,0.0,60,180,2.5 );

        // tableau polymorphique d'objeJeu
        ObjetJeu[] elements=new ObjetJeu[2];
        elements[0]=oiseau;
        elements[1]=tuyau;

        oiseau.flap();

        // on met à jour touts les objets en 3 étapes
        for (int etape=1; etape<=3; etape++){
            System.out.println("--- Étape " + etape + " ---");
            for (int i=0; i<elements.length;i++){
                elements[i].mettreAJour();
            }
            oiseau.afficherEtat();
            tuyau.afficherEtat();
        }
    }
}
