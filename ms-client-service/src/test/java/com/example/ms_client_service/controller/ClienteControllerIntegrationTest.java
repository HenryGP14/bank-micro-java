package com.example.ms_client_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ms_client_service.dto.ClienteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void crearYConsultarCliente() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setNombre("Jose Lema");
        request.setGenero("M");
        request.setEdad(35);
        request.setIdentificacion("1234567890");
        request.setDireccion("Otavalo sn y principal");
        request.setTelefono("098254785");
        request.setContrasenia("1234");
        request.setEstado(true);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Cliente registrado exitosamente"))
                .andExpect(jsonPath("$.data.nombre").value("Jose Lema"))
                .andExpect(jsonPath("$.data.identificacion").value("1234567890"));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void noPermiteRegistrarClienteConIdentificacionDuplicada() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setNombre("Jose Lema");
        request.setGenero("M");
        request.setEdad(35);
        request.setIdentificacion("1234567890");
        request.setDireccion("Otavalo sn y principal");
        request.setTelefono("098254785");
        request.setContrasenia("1234");
        request.setEstado(true);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Lo sentimos, ya existe un cliente registrado con este numero de identificacion."));
    }
}
