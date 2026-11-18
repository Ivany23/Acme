package acme.seguranca;

import acme.modelo.Papel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indica que papéis podem usar um endpoint da API.
 * Um endpoint sem esta anotação é recusado (negado por omissão).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Acesso {

    /** Papéis autorizados. */
    Papel[] value() default {};

    /** Se verdadeiro, qualquer funcionário autenticado (todos os papéis excepto CLIENTE) é autorizado. */
    boolean funcionarios() default false;
}
