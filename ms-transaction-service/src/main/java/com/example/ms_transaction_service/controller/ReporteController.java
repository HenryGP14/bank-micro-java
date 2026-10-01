package com.example.ms_transaction_service.controller;

import com.example.ms_transaction_service.dto.BasicResponse;
import com.example.ms_transaction_service.dto.ReporteEstadoCuentaResponse;
import com.example.ms_transaction_service.service.ReporteService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Reporte de estado de cuenta por rango de fechas. Es el unico recurso del servicio sin auth
 * propia: se identifica al cliente por clienteId o por numero de identificacion (uno de los
 * dos es obligatorio), pensado para que lo pueda invocar tanto Postman/un cliente externo como
 * otro proceso interno que ya tenga la identificacion a mano y no quiera pasar por login.
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping
    public BasicResponse<ReporteEstadoCuentaResponse> generarReporte(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String identificacion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        // clienteId tiene prioridad si llegan los dos; identificacion es el camino alterno
        ReporteEstadoCuentaResponse reporte;
        if (clienteId != null) {
            reporte = reporteService.generarReporte(clienteId, fechaInicio, fechaFin);
        } else if (identificacion != null && !identificacion.isBlank()) {
            reporte = reporteService.generarReportePorIdentificacion(identificacion, fechaInicio, fechaFin);
        } else {
            throw new IllegalArgumentException("Debe indicar clienteId o identificacion");
        }
        return BasicResponse.success(HttpStatus.OK.value(), "Reporte generado exitosamente", reporte);
    }
}
