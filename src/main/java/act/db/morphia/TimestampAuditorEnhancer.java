package act.db.morphia;

/*-
 * #%L
 * ACT Morphia Module
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.app.App;
import act.asm.AnnotationVisitor;
import act.asm.Type;
import act.db.meta.*;
import act.util.AppByteCodeEnhancer;
import org.osgl.util.S;

// add @EntityListeners annotation to entity classes that has timestamp fields
public class TimestampAuditorEnhancer extends AppByteCodeEnhancer<TimestampAuditorEnhancer> {

    private EntityMetaInfoRepo metaInfoRepo;
    private String className;
    private EntityFieldMetaInfo createdAt;
    private EntityFieldMetaInfo lastModifiedAt;
    private boolean entityListenersFound;

    public TimestampAuditorEnhancer() {
        super(S.F.startsWith("act.").negate());
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        metaInfoRepo = app.entityMetaInfoRepo();
        return super.app(app);
    }

    @Override
    protected Class<TimestampAuditorEnhancer> subClass() {
        return TimestampAuditorEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String classDesc = "L" + name + ";";
        className = Type.getType(classDesc).getClassName();
        EntityClassMetaInfo classMetaInfo = metaInfoRepo.classMetaInfo(className);
        if (null != classMetaInfo) {
            createdAt = classMetaInfo.createdAtField();
            lastModifiedAt = classMetaInfo.lastModifiedAtField();
            entityListenersFound = classMetaInfo.hasEntityListeners();
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        if (!entityListenersFound && (null != createdAt || null != lastModifiedAt)) {
            AnnotationVisitor av = super.visitAnnotation("Lorg/mongodb/morphia/annotations/EntityListeners;", true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, Type.getType("Lact/db/morphia/MorphiaAuditHelper;"));
            av1.visitEnd();
            av.visitEnd();
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if ((null != createdAt || null != lastModifiedAt) && entityListenersFound && "Lorg/mongodb/morphia/annotations/EntityListeners;".equals(desc)) {
            return new AnnotationVisitor(ASM5, av) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    return new AnnotationVisitor(ASM5, super.visitArray(name)) {
                        @Override
                        public void visitEnd() {
                            visit(null, Type.getType("Lact/db/morphia/MorphiaAuditHelper;"));
                            super.visitEnd();
                        }
                    };
                }
            };
        }
        return av;
    }
}
