package org.nakedobjects.persistence.sql;

import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObjects;
import org.nakedobjects.object.ObjectPerstsistenceException;
import org.nakedobjects.object.Oid;
import org.nakedobjects.object.persistence.OidGenerator;
import org.nakedobjects.utility.NakedObjectRuntimeException;
import org.nakedobjects.utility.configuration.ComponentLoader;


public class SqlOidGenerator implements OidGenerator {
    private static final String BASE_NAME = "nakedobjects.sql-object-store";
    private long number;
    private DatabaseConnectorFactory connectorFactory;

    public void init() {
        String connectorClass = NakedObjects.getConfiguration().getString(BASE_NAME + ".connector");
        connectorFactory = (DatabaseConnectorFactory) ComponentLoader.loadComponent(connectorClass,
                DatabaseConnectorFactory.class);
        DatabaseConnector db = connectorFactory.createConnector();
        try {
            db.open();
            if (!db.hasTable("no_serial_id")) {
                db.update("create table \"no_serial_id\" (\"number\" INTEGER)");
                db.update("insert into \"no_serial_id\" values (1)");
            }

            Results rs = db.select("select \"number\" from \"no_serial_id\"");
            rs.next();
            number = rs.getLong("number");
            rs.close();
            db.close();
        } catch (ObjectPerstsistenceException e) {
            throw new NakedObjectRuntimeException(e);
        }
    }

    public String name() {
        return "Sql Oids";
    }

    public synchronized Oid next(Naked object) {
        String className = object.getSpecification().getFullName();
        IntegerPrimaryKey primaryKey = new IntegerPrimaryKey((int) number++);
        return new SqlOid(className, primaryKey);
    }

    public void shutdown() {
        DatabaseConnector db = connectorFactory.createConnector();
        try {
            db.open();
            db.update("update \"no_serial_id\" set \"number\" = " + number);
        } catch (ObjectPerstsistenceException e) {
            throw new NakedObjectRuntimeException(e);
        } finally {
            try {
                db.close();
            } catch (SqlObjectStoreException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business objects directly to the user.
 * Copyright (C) 2000 - 2005 Naked Objects Group Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address of Naked Objects Group is
 * Kingsway House, 123 Goldworth Road, Woking GU21 1NR, UK).
 */