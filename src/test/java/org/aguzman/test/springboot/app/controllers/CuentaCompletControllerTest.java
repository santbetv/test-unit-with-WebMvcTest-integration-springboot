package org.aguzman.test.springboot.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aguzman.test.springboot.app.models.Banco;
import org.aguzman.test.springboot.app.models.Cuenta;
import org.aguzman.test.springboot.app.models.TransaccionDto;
import org.aguzman.test.springboot.app.repositories.BancoRepository;
import org.aguzman.test.springboot.app.repositories.CuentaRepository;
import org.aguzman.test.springboot.app.services.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.aguzman.test.springboot.app.Datos.crearCuenta001;
import static org.aguzman.test.springboot.app.Datos.crearCuenta002;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CuentaController.class,
        CuentaService.class})
class CuentaCompletControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CuentaRepository cuentaRepository;

    @MockBean
    private BancoRepository bancoRepository;

    @InjectMocks
    private ObjectMapper objectMapper;

    @Test
    void testDetalle() throws Exception {
        // Given
        when(cuentaRepository.findById(1L)).thenReturn(crearCuenta001());

        // When
        mvc.perform(get("/api/cuentas/1").contentType(MediaType.APPLICATION_JSON))
        // Then
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.persona").value("Andrés"))
                .andExpect(jsonPath("$.saldo").value("1000"));

        verify(cuentaRepository).findById(1L);
    }

    @Test
    void testTransferir() throws Exception {

        // Given
        TransaccionDto dto = new TransaccionDto();
        dto.setCuentaOrigenId(1L);
        dto.setCuentaDestinoId(2L);
        dto.setMonto(new BigDecimal("100"));
        dto.setBancoId(1L);

        System.out.println(objectMapper.writeValueAsString(dto));

        when(cuentaRepository.findById(1L)).thenReturn(crearCuenta001());
        when(cuentaRepository.findById(2L)).thenReturn(crearCuenta002());

        // Mockear el banco
        Banco banco = new Banco();
        banco.setId(1L);
        banco.setNombre("Banco Test");
        banco.setTotalTransferencias(0);
        when(bancoRepository.findById(1L)).thenReturn(Optional.of(banco));

        Map<String, Object> response = new HashMap<>();
        response.put("date", LocalDate.now().toString());
        response.put("status", "OK");
        response.put("mensaje", "Transferencia realizada con éxito!");
        response.put("transaccion", dto);

        System.out.println(objectMapper.writeValueAsString(response));

        // When
        mvc.perform(post("/api/cuentas/transferir")
                .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))

        // Then
        .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.mensaje").value("Transferencia realizada con éxito!"))
                .andExpect(jsonPath("$.transaccion.cuentaOrigenId").value(dto.getCuentaOrigenId()))
        .andExpect(content().json(objectMapper.writeValueAsString(response)));

        // Verificar que llama a los tres repositorios
        verify(cuentaRepository, times(1)).findById(1L);
        verify(cuentaRepository, times(1)).findById(2L);
        verify(bancoRepository).findById(1L);

    }

    @Test
    void testListar() throws Exception {
        // Given
        List<Cuenta> cuentas = Arrays.asList(crearCuenta001().orElseThrow(),
                crearCuenta002().orElseThrow()
        );
        when(cuentaRepository.findAll()).thenReturn(cuentas);

        // When
        mvc.perform(get("/api/cuentas").contentType(MediaType.APPLICATION_JSON))
        // Then
        .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].persona").value("Andrés"))
                .andExpect(jsonPath("$[1].persona").value("Jhon"))
                .andExpect(jsonPath("$[0].saldo").value("1000"))
                .andExpect(jsonPath("$[1].saldo").value("2000"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(content().json(objectMapper.writeValueAsString(cuentas)));

        verify(cuentaRepository).findAll();
    }

    @Test
    void testGuardar() throws Exception {
        // Given
        Cuenta cuenta = new Cuenta(null, "Pepe", new BigDecimal("3000"));
        when(cuentaRepository.save(any())).then(invocation ->{
            Cuenta c = invocation.getArgument(0);
            c.setId(3L);
            return c;
        });

        // when
        mvc.perform(post("/api/cuentas").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuenta)))
        // Then
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.persona", is("Pepe")))
                .andExpect(jsonPath("$.saldo", is(3000)));
        verify(cuentaRepository).save(any());

    }
}
