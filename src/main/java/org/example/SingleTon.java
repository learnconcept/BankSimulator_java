package org.example;

public class SingleTon {

    // static instance variable
    private static SingleTon instance;
    // private constructor to avoid instantiation
    private SingleTon(){

    }

    // Need to provide a static method

    public static SingleTon getInstance(){
        if(instance == null){
            instance = new SingleTon();
        }
        return instance;
    }

}
