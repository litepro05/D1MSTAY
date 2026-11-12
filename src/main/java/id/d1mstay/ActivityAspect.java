package id.d1mstay;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActivityAspect {
    private static final Logger log = LoggerFactory.getLogger(ActivityAspect.class);

    // Hanya operasi perubahan data. Argumen dan isi formulir tidak dicatat.
    @Around("execution(public * id.d1mstay.KosService.*(..)) && "
        + "@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object record(ProceedingJoinPoint operation) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication == null ? "sistem" : authentication.getName();
        String action = operation.getSignature().getName();
        long start = System.nanoTime();
        try {
            Object result = operation.proceed();
            log.info("[Aktivitas] pengguna={} aksi={} hasil=selesai durasiMs={}",
                user, action, elapsed(start));
            return result;
        } catch (Throwable error) {
            log.warn("[Aktivitas] pengguna={} aksi={} hasil=gagal sebab={} durasiMs={}",
                user, action, error.getClass().getSimpleName(), elapsed(start));
            throw error;
        }
    }

    private long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
