package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jef.database.dialect.type.ColumnMapping;

/**
 * 扩展数据类型的注解，可用于支持新的数据映射方式
 * 
 * @author jiyi
 *
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface Type {
	/**
	 * 自定义的java和数据库类型映射实现
	 * @return
	 */
	Class<? extends ColumnMapping> value();

	/**
	 * Any configuration parameters for the named type.
	 */
	Parameter[] parameters() default {};
}
