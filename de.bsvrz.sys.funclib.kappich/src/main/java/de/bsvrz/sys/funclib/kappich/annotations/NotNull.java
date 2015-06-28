/*
 * Copyright 2012 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.sys.funclib.kappich.
 * 
 * de.bsvrz.sys.funclib.kappich is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.sys.funclib.kappich is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with de.bsvrz.sys.funclib.kappich; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.sys.funclib.kappich.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Zeigt an, dass eine Funktion, Variable oder Feld den Wert <code>null</code> <strong>nicht</strong> enthalten bzw. zurückgeben darf.
 * Lässt sich in den Idea-Inspections unter @Nullable/@NotNull einrichten um entsprechende Warnungen über mögliche
 * NullPointerExceptions zu erhalten. Dient außerdem der Code-Dokumentation.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 13237 $
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(value={FIELD, LOCAL_VARIABLE, PARAMETER, METHOD})
public @interface NotNull {

}
