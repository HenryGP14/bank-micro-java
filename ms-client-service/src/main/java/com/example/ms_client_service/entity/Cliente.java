package com.example.ms_client_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "id_cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente extends Persona {

    @JsonIgnore
    @Column(nullable = false)
    private String contrasenia;

    @Column(nullable = false)
    private Boolean estado = true;

    public boolean isActivo() {
        return Boolean.TRUE.equals(estado);
    }
}
