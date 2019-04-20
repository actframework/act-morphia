package act.aaa.model;

/*-
 * #%L
 * ACT Morphia Module
 * %%
 * Copyright (C) 2015 - 2019 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.asm.AnnotationVisitor;
import act.asm.ClassVisitor;
import act.asm.FieldVisitor;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

/**
 * Add {@link org.mongodb.morphia.annotations.Transient} to
 * specific fields in aaa models
 */
public class MorphiaAAAObjectBaseEnhancer extends AppByteCodeEnhancer<MorphiaAAAObjectBaseEnhancer> {

    private static final $.Predicate<String> PREDICATE = new $.Predicate<String>() {
        @Override
        public boolean test(String s) {
            return s.equalsIgnoreCase("org.osgl.aaa.impl.AAAObjectBase")
                    || s.startsWith("org.osgl.aaa.impl.SimpleP")
                    ||s.startsWith("org.osgl.aaa.impl.SimpleR");
        }
    };

    public MorphiaAAAObjectBaseEnhancer() {
        super(PREDICATE);
    }

    public MorphiaAAAObjectBaseEnhancer(ClassVisitor cv) {
        super(PREDICATE, cv);
    }

    @Override
    protected Class<MorphiaAAAObjectBaseEnhancer> subClass() {
        return MorphiaAAAObjectBaseEnhancer.class;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if (name.equals("props") || name.equals("implied")) {
            return new FieldVisitor(ASM5, fv) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    visitAnnotation("Lorg/mongodb/morphia/annotations/Transient;", true);
                }
            };
        }
        return fv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }
}
