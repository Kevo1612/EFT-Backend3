package cl.bancoxyz.bffcajero.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Segundo factor del canal: ademas del JWT, la peticion debe venir de un cajero fisico registrado
 * (header X-Cajero-Id). En produccion este control se reforzaria con mTLS (certificado de cliente
 * instalado en cada cajero). El id del cajero queda en el MDC para que aparezca en todos los logs (auditoria).
 */
public class TerminalCajeroFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Cajero-Id";
    private static final Logger audit = LoggerFactory.getLogger("AUDITORIA_CAJERO");

    private final CajeroProperties props;

    public TerminalCajeroFilter(CajeroProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/cajero/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String cajero = req.getHeader(HEADER);
        if (cajero == null || !props.terminalesAutorizados().contains(cajero)) {
            audit.warn("RECHAZADO terminal='{}' ip={} uri={}", cajero, req.getRemoteAddr(), req.getRequestURI());
            res.setStatus(HttpStatus.FORBIDDEN.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"status\":403,\"mensaje\":\"Terminal no autorizado\"}");
            return;
        }
        MDC.put("cajero", cajero);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("cajero");
        }
    }
}
