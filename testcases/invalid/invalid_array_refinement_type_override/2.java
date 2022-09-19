class A{
    //`@ def_type NatArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x[i] >= 0)};
    //`@ def_type NatEvenArray = {NatArray x | (\forall int i; 0 <= i && i < x.length; x[i] % 2 == 0)};
    int[] /*`@refinement_type NatArray*/ x1;
    int[] x2;

    int[] test(){
        //@assert false;
        return new int[2];
    }

    int[]/*`@ refinement_type NatArray*/ test2(int[]/*`@ refinement_type NatEvenArray*/ a){
        //@assert false;
        return a;
    }
    
}

class C extends A{
    /*`@override_refinement_type NatEvenArray x2;*/
    
}