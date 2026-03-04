package org.example.services;

import java.sql.SQLException;
import java.util.List;

public interface CRUD<T>{
    void ajouter(T t) throws SQLException;
    void modifie(int id, T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<T> afficher() throws SQLException;
}
