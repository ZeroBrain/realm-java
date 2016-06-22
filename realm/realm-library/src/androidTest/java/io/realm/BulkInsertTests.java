/*
 * Copyright 2014-2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.realm.entities.AllJavaTypes;
import io.realm.entities.AllTypes;
import io.realm.entities.AllTypesPrimaryKey;
import io.realm.entities.CyclicType;
import io.realm.entities.Dog;
import io.realm.entities.DogPrimaryKey;
import io.realm.entities.NoPrimaryKeyWithPrimaryKeyObjectRelation;
import io.realm.entities.NullTypes;
import io.realm.entities.PrimaryKeyAsBoxedShort;
import io.realm.entities.PrimaryKeyAsLong;
import io.realm.entities.PrimaryKeyAsString;
import io.realm.entities.PrimaryKeyWithNoPrimaryKeyObjectRelation;
import io.realm.entities.pojo.AllTypesRealmModel;
import io.realm.entities.pojo.InvalidRealmModel;
import io.realm.exceptions.RealmException;
import io.realm.rule.TestRealmConfigurationFactory;

import static io.realm.internal.test.ExtraTests.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BulkInsertTests {
    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();

    private Realm realm;
    private RealmConfiguration realmConfig;

    @Before
    public void setUp() {
        realmConfig = configFactory.createConfiguration();
        realm = Realm.getInstance(realmConfig);
    }

    @After
    public void tearDown() {
        if (realm != null) {
            realm.close();
        }
    }

    @Test
    public void insertToRealm() {
        AllJavaTypes obj = new AllJavaTypes();
        obj.setFieldIgnored("cookie");
        obj.setFieldLong(42);
        obj.setFieldString("obj1");

        RealmList<AllJavaTypes> list = new RealmList<AllJavaTypes>();
        list.add(obj);

        Date date = new Date();

        AllJavaTypes allTypes = new AllJavaTypes();
        allTypes.setFieldString("String");
        allTypes.setFieldLong(1l);
        allTypes.setFieldFloat(1f);
        allTypes.setFieldDouble(1d);
        allTypes.setFieldBoolean(true);
        allTypes.setFieldDate(date);
        allTypes.setFieldBinary(new byte[]{1, 2, 3});
        allTypes.setFieldObject(obj);
        allTypes.setFieldList(list);

        realm.beginTransaction();
        realm.insert(allTypes);
        realm.commitTransaction();

        AllJavaTypes realmTypes = realm.where(AllJavaTypes.class).findFirst();

        assertNotSame(allTypes, realmTypes); // Objects should not be considered equal
        assertEquals(allTypes.getFieldString(), realmTypes.getFieldString()); // But they contain the same data
        assertEquals(allTypes.getFieldLong(), realmTypes.getFieldLong());
        assertEquals(allTypes.getFieldFloat(), realmTypes.getFieldFloat(), 0);
        assertEquals(allTypes.getFieldDouble(), realmTypes.getFieldDouble(), 0);
        assertEquals(allTypes.isFieldBoolean(), realmTypes.isFieldBoolean());
        assertEquals(allTypes.getFieldDate(), realmTypes.getFieldDate());
        assertArrayEquals(allTypes.getFieldBinary(), realmTypes.getFieldBinary());
        assertEquals(allTypes.getFieldObject().getFieldString(), obj.getFieldString());
        assertEquals(list.size(), realmTypes.getFieldList().size());
        assertEquals(list.get(0).getFieldString(), realmTypes.getFieldList().get(0).getFieldString());
        assertEquals(list.get(0).getFieldLong(), realmTypes.getFieldList().get(0).getFieldLong());
        assertNull(realmTypes.getFieldList().get(0).getFieldIgnored());


        // make sure Dog was not inserted twice in the recursive process
        assertEquals(2, realm.where(AllJavaTypes.class).findAll().size());
    }

    @Test
    public void insertToRealm_RealmModel() {
        AllTypesRealmModel allTypes = new AllTypesRealmModel();
        allTypes.columnLong = 10;
        allTypes.columnBoolean = false;
        allTypes.columnBinary = new byte[]{1, 2, 3};
        allTypes.columnDate = new Date();
        allTypes.columnDouble = 3.1415;
        allTypes.columnFloat = 1.234567f;
        allTypes.columnString = "test data";
        allTypes.columnByte = 0b0010_1010;

        realm.beginTransaction();
        realm.insert(allTypes);
        realm.commitTransaction();

        AllTypesRealmModel first = realm.where(AllTypesRealmModel.class).findFirst();
        assertEquals(allTypes.columnString, first.columnString);
        assertEquals(allTypes.columnLong, first.columnLong);
        assertEquals(allTypes.columnBoolean, first.columnBoolean);
        assertArrayEquals(allTypes.columnBinary, first.columnBinary);
        assertEquals(allTypes.columnDate, first.columnDate);
        assertEquals(allTypes.columnDouble, first.columnDouble, 0.0000001);
        assertEquals(allTypes.columnFloat, first.columnFloat, 0.0000001);
        assertEquals(allTypes.columnByte, first.columnByte);
    }

    @Test
    public void insertToRealm_InvalidRealmModel() {
        InvalidRealmModel invalidRealmModel = new InvalidRealmModel();

        realm.beginTransaction();
        try {
            realm.insert(invalidRealmModel);
            fail("Expected Missing Proxy Class Exception");
        } catch (RealmException ignored) {
        } finally {
            realm.cancelTransaction();
        }
    }

    @Test
    public void insertOrUpdateToRealm_NullTypes() {
        NullTypes nullTypes1 = new NullTypes();
        nullTypes1.setId(1);
        nullTypes1.setFieldIntegerNull(1);
        nullTypes1.setFieldFloatNull(2F);
        nullTypes1.setFieldDoubleNull(3D);
        nullTypes1.setFieldBooleanNull(true);
        nullTypes1.setFieldStringNull("4");
        nullTypes1.setFieldDateNull(new Date(12345));

        realm.beginTransaction();
        realm.insert(nullTypes1);
        realm.commitTransaction();

        NullTypes first = realm.where(NullTypes.class).findFirst();

        assertEquals(nullTypes1.getId(), first.getId());
        assertEquals(nullTypes1.getFieldIntegerNull(), first.getFieldIntegerNull());
        assertEquals(nullTypes1.getFieldFloatNull(), first.getFieldFloatNull());
        assertEquals(nullTypes1.getFieldDoubleNull(), first.getFieldDoubleNull());
        assertEquals(nullTypes1.getFieldBooleanNull(), first.getFieldBooleanNull());
        assertEquals(nullTypes1.getFieldStringNull(), first.getFieldStringNull());
        assertEquals(nullTypes1.getFieldDateNull(), first.getFieldDateNull());

        NullTypes nullTypes2 = new NullTypes();
        nullTypes2.setId(2);

        NullTypes nullTypes3 = new NullTypes();
        nullTypes3.setId(3);

        nullTypes1 = new NullTypes();
        nullTypes1.setId(1);
        nullTypes1.setFieldIntegerNull(null);
        nullTypes1.setFieldFloatNull(null);
        nullTypes1.setFieldDoubleNull(null);
        nullTypes1.setFieldBooleanNull(null);
        nullTypes1.setFieldStringNull(null);
        nullTypes1.setFieldDateNull(null);
        nullTypes1.setFieldListNull(new RealmList<NullTypes>());
        nullTypes1.getFieldListNull().add(nullTypes2);
        nullTypes1.getFieldListNull().add(nullTypes3);

        OrderedRealmCollection<NullTypes> collection = new RealmList<NullTypes>();
        collection.add(nullTypes2);
        collection.add(nullTypes1);
        collection.add(nullTypes3);

        realm.beginTransaction();
        realm.insertOrUpdate(collection);
        realm.commitTransaction();

        first = realm.where(NullTypes.class).equalTo("id", 1).findFirst();

        assertEquals(nullTypes1.getId(), first.getId());
        assertNull(first.getFieldIntegerNull());
        assertNull(first.getFieldFloatNull());
        assertNull(first.getFieldDoubleNull());
        assertNull(first.getFieldBooleanNull());
        assertNull(first.getFieldStringNull());
        assertNull(first.getFieldDateNull());
        assertEquals(2, first.getFieldListNull().size());
        assertEquals(2, first.getFieldListNull().get(0).getId());
        assertEquals(3, first.getFieldListNull().get(1).getId());
    }

    @Test
    public void insertToRealm_CyclicType() {
        CyclicType oneCyclicType = new CyclicType();
        oneCyclicType.setName("One");
        CyclicType anotherCyclicType = new CyclicType();
        anotherCyclicType.setName("Two");
        oneCyclicType.setObject(anotherCyclicType);
        anotherCyclicType.setObject(oneCyclicType);

        realm.beginTransaction();
        realm.insert(oneCyclicType);
        realm.commitTransaction();

        CyclicType realmObject = realm.where(CyclicType.class).findFirst();
        assertEquals("One", realmObject.getName());
        assertEquals("Two", realmObject.getObject().getName());
        assertEquals(2, realm.where(CyclicType.class).count());
    }

    @Test
    public void insertToRealm_NullPrimaryKey() {
        PrimaryKeyAsString primaryKeyAsString = new PrimaryKeyAsString();
        primaryKeyAsString.setId(19);

        realm.beginTransaction();
        realm.insertOrUpdate(primaryKeyAsString);
        realm.commitTransaction();

        primaryKeyAsString = realm.where(PrimaryKeyAsString.class).isNull("name").findFirst();
        assertNull(primaryKeyAsString.getName());
        assertEquals(19, primaryKeyAsString.getId());

        PrimaryKeyAsBoxedShort primaryKeyAsShort = new PrimaryKeyAsBoxedShort();
        primaryKeyAsShort.setName("42");

        realm.beginTransaction();
        realm.insertOrUpdate(primaryKeyAsShort);
        realm.commitTransaction();

        primaryKeyAsShort = realm.where(PrimaryKeyAsBoxedShort.class).isNull("id").findFirst();
        assertNull(primaryKeyAsShort.getId());
        assertEquals("42", primaryKeyAsShort.getName());
    }

    @Test
    public void insertOrUpdateToRealm() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                AllTypesPrimaryKey obj = new AllTypesPrimaryKey();
                obj.setColumnString("Foo");
                obj.setColumnLong(1);
                obj.setColumnFloat(1.23F);
                obj.setColumnDouble(1.234D);
                obj.setColumnBoolean(false);
                obj.setColumnBinary(new byte[]{1, 2, 3});
                obj.setColumnDate(new Date(1000));
                obj.setColumnRealmObject(new DogPrimaryKey(1, "Dog1"));
                obj.setColumnRealmList(new RealmList<DogPrimaryKey>(new DogPrimaryKey(2, "Dog2")));
                obj.setColumnBoxedBoolean(true);
                realm.insert(obj);

                AllTypesPrimaryKey obj2 = new AllTypesPrimaryKey();
                obj2.setColumnString("Bar");
                obj2.setColumnLong(1);
                obj2.setColumnFloat(2.23F);
                obj2.setColumnDouble(2.234D);
                obj2.setColumnBoolean(true);
                obj2.setColumnBinary(new byte[]{2, 3, 4});
                obj2.setColumnDate(new Date(2000));
                obj2.setColumnRealmObject(new DogPrimaryKey(3, "Dog3"));
                obj2.setColumnRealmList(new RealmList<DogPrimaryKey>(new DogPrimaryKey(4, "Dog4")));
                obj2.setColumnBoxedBoolean(false);
                realm.insertOrUpdate(obj2);
            }
        });

        assertEquals(1, realm.where(AllTypesPrimaryKey.class).count());
        AllTypesPrimaryKey obj = realm.where(AllTypesPrimaryKey.class).findFirst();

        // Check that the the only element has all its properties updated
        assertEquals("Bar", obj.getColumnString());
        assertEquals(1, obj.getColumnLong());
        assertEquals(2.23F, obj.getColumnFloat(), 0);
        assertEquals(2.234D, obj.getColumnDouble(), 0);
        assertEquals(true, obj.isColumnBoolean());
        assertArrayEquals(new byte[]{2, 3, 4}, obj.getColumnBinary());
        assertEquals(new Date(2000), obj.getColumnDate());
        assertEquals("Dog3", obj.getColumnRealmObject().getName());
        assertEquals(1, obj.getColumnRealmList().size());
        assertEquals("Dog4", obj.getColumnRealmList().get(0).getName());
        assertEquals(4, realm.where(DogPrimaryKey.class).findAll().size());
        assertFalse(obj.getColumnBoxedBoolean());
    }

    @Test
    public void insertToRealm_list() {
        Dog dog1 = new Dog();
        dog1.setName("Dog 1");
        Dog dog2 = new Dog();
        dog2.setName("Dog 2");
        RealmList<Dog> list = new RealmList<Dog>();
        list.addAll(Arrays.asList(dog1, dog2));

        realm.beginTransaction();
        realm.insert(list);
        realm.commitTransaction();


        RealmResults<Dog> copiedList = realm.where(Dog.class).findAll();

        assertEquals(2, copiedList.size());
        assertEquals(dog1.getName(), copiedList.get(0).getName());
        assertEquals(dog2.getName(), copiedList.get(1).getName());
    }

    @Test
    public void insertOrUpdateToRealm_list() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                PrimaryKeyAsLong obj = new PrimaryKeyAsLong();
                obj.setId(1);
                obj.setName("Foo");
                realm.copyToRealm(obj);

                PrimaryKeyAsLong obj2 = new PrimaryKeyAsLong();
                obj2.setId(1);
                obj2.setName("Bar");

                PrimaryKeyAsLong obj3 = new PrimaryKeyAsLong();
                obj3.setId(1);
                obj3.setName("Baz");

                realm.insertOrUpdate(Arrays.asList(obj2, obj3));
            }
        });

        assertEquals(1, realm.where(PrimaryKeyAsLong.class).count());
        assertEquals("Baz", realm.where(PrimaryKeyAsLong.class).findFirst().getName());
    }

    @Test
    public void insertOrUpdate_mixingPrimaryKeyAndNoPrimaryKeyModels() {
        AllTypes objB_no_pk = new AllTypes();
        objB_no_pk.setColumnString("B");

        PrimaryKeyWithNoPrimaryKeyObjectRelation objA_pk = new PrimaryKeyWithNoPrimaryKeyObjectRelation();
        objA_pk.setColumnString("A");
        objA_pk.setColumnRealmObjectNoPK(objB_no_pk);

        realm.beginTransaction();
        realm.insert(objA_pk);
        realm.commitTransaction();

        RealmResults<PrimaryKeyWithNoPrimaryKeyObjectRelation> all = realm.where(PrimaryKeyWithNoPrimaryKeyObjectRelation.class).findAll();
        assertEquals(1, all.size());
        assertEquals("A", all.get(0).getColumnString());
        assertEquals(8, all.get(0).getColumnInt());
        assertNotNull(all.get(0).getColumnRealmObjectNoPK());
        assertEquals("B", all.get(0).getColumnRealmObjectNoPK().getColumnString());
        assertEquals(1, realm.where(AllTypes.class).findAll().size());

        objA_pk.setColumnInt(42);
        objB_no_pk.setColumnString("updated B");

        realm.beginTransaction();
        realm.insertOrUpdate(objA_pk);
        realm.commitTransaction();

        all = realm.where(PrimaryKeyWithNoPrimaryKeyObjectRelation.class).findAll();
        assertEquals(1, all.size());
        assertEquals("A", all.get(0).getColumnString());
        assertEquals(42, all.get(0).getColumnInt());
        assertNotNull(all.get(0).getColumnRealmObjectNoPK());
        assertEquals("updated B", all.get(0).getColumnRealmObjectNoPK().getColumnString());
        // since AllTypes doesn't have a PK we now have two instances
        assertEquals(2, realm.where(AllTypes.class).findAll().size());
    }


    @Test
    public void insertOrUpdate_mixingNoPrimaryKeyAndPrimaryKeyModels() {
        AllTypesPrimaryKey objB_pk = new AllTypesPrimaryKey();
        objB_pk.setColumnLong(7);
        objB_pk.setColumnString("B");

        NoPrimaryKeyWithPrimaryKeyObjectRelation objA_no_pk = new NoPrimaryKeyWithPrimaryKeyObjectRelation();
        objA_no_pk.setColumnRealmObjectPK(objB_pk);
        objA_no_pk.setColumnString("A");

        realm.beginTransaction();
        realm.insert(objA_no_pk);
        realm.commitTransaction();

        RealmResults<NoPrimaryKeyWithPrimaryKeyObjectRelation> all = realm.where(NoPrimaryKeyWithPrimaryKeyObjectRelation.class).findAll();
        assertEquals(1, all.size());
        assertEquals("A", all.get(0).getColumnString());
        assertEquals(8, all.get(0).getColumnInt());
        assertNotNull(all.get(0).getColumnRealmObjectPK());
        assertEquals(7, all.get(0).getColumnRealmObjectPK().getColumnLong());
        assertEquals("B", all.get(0).getColumnRealmObjectPK().getColumnString());
        assertEquals(1, realm.where(AllTypesPrimaryKey.class).findAll().size());

        objA_no_pk.setColumnString("different A");
        objA_no_pk.setColumnInt(42);//should insert a new instance
        // update (since it has a PK) now both AllTypesPrimaryKey points to the same objB_pk instance
        objB_pk.setColumnString("updated B");

        realm.beginTransaction();
        realm.insertOrUpdate(objA_no_pk);
        realm.commitTransaction();

        all = realm.where(NoPrimaryKeyWithPrimaryKeyObjectRelation.class).findAllSorted("columnString");
        assertEquals(2, all.size());
        assertEquals("A", all.get(0).getColumnString());
        assertEquals(8, all.get(0).getColumnInt());
        assertEquals("different A", all.get(1).getColumnString());
        assertEquals(42, all.get(1).getColumnInt());

        assertNotNull(all.get(0).getColumnRealmObjectPK());
        assertNotNull(all.get(1).getColumnRealmObjectPK());

        assertEquals(7, all.get(0).getColumnRealmObjectPK().getColumnLong());
        assertEquals(7, all.get(1).getColumnRealmObjectPK().getColumnLong());
        assertEquals("updated B", all.get(0).getColumnRealmObjectPK().getColumnString());
        assertEquals("updated B", all.get(1).getColumnRealmObjectPK().getColumnString());
        assertEquals(1, realm.where(AllTypesPrimaryKey.class).findAll().size());
    }

    @Test
    public void insertOrUpdate_mixingPrimaryAndNoPrimaryKeyList() {
        NoPrimaryKeyWithPrimaryKeyObjectRelation objA_no_pk = new NoPrimaryKeyWithPrimaryKeyObjectRelation();
        objA_no_pk.setColumnString("A");
        NoPrimaryKeyWithPrimaryKeyObjectRelation objB_no_pk = new NoPrimaryKeyWithPrimaryKeyObjectRelation();
        objB_no_pk.setColumnString("B");
        AllTypesPrimaryKey objC_pk = new AllTypesPrimaryKey();
        objC_pk.setColumnLong(7);
        objC_pk.setColumnString("C");
        AllTypesPrimaryKey objD_pk = new AllTypesPrimaryKey();
        objD_pk.setColumnLong(7);
        objD_pk.setColumnString("D");

        objA_no_pk.setColumnRealmObjectPK(objC_pk);
        objB_no_pk.setColumnRealmObjectPK(objD_pk);

        ArrayList<NoPrimaryKeyWithPrimaryKeyObjectRelation> objects = new ArrayList<NoPrimaryKeyWithPrimaryKeyObjectRelation>(2);
        objects.add(objA_no_pk);
        objects.add(objB_no_pk);

        realm.beginTransaction();
        realm.insertOrUpdate(objects);
        realm.commitTransaction();

        RealmResults<NoPrimaryKeyWithPrimaryKeyObjectRelation> all = realm.where(NoPrimaryKeyWithPrimaryKeyObjectRelation.class).findAllSorted("columnString", Sort.DESCENDING);
        assertEquals(2, all.size());
        assertEquals("B", all.get(0).getColumnString());
        assertEquals("A", all.get(1).getColumnString());

        assertNotNull(all.get(0).getColumnRealmObjectPK());
        assertNotNull(all.get(1).getColumnRealmObjectPK());

        assertEquals("D", all.get(0).getColumnRealmObjectPK().getColumnString());
        assertEquals("D", all.get(1).getColumnRealmObjectPK().getColumnString());

        assertEquals(1, realm.where(AllTypesPrimaryKey.class).findAll().size());
    }

    //any omitted argument should not end in a SIGSEGV but an exception

    @Test
    public void testObjectNull() {
        AllTypes nullObject = null;

        realm.beginTransaction();
        try {
            realm.insert(nullObject);
            fail("Should trigger NullPointerException");
        } catch (NullPointerException ignore) {

        } finally {
            realm.cancelTransaction();
        }
    }

    @Test
    public void testListNull() {
        List<AllTypes> nullObjects = null;

        realm.beginTransaction();
        try {
            realm.insert(nullObjects);
            fail("Should trigger NullPointerException");
        } catch (NullPointerException ignore) {

        } finally {
            realm.cancelTransaction();
        }
    }

    @Test
    public void testListNullElement() {
        Dog dog1 = new Dog();
        dog1.setName("Dog 1");
        Dog dog2 = new Dog();
        dog2.setName("Dog 2");
        ArrayList<Dog> list = new ArrayList<Dog>();
        list.addAll(Arrays.asList(dog1, null, dog2));

        realm.beginTransaction();
        try {
            realm.insert(list);
        } catch (NullPointerException ignore) {

        } finally {
            realm.cancelTransaction();
        }
    }

    @Test
    public void testManagedObject() {
        AllJavaTypes obj = new AllJavaTypes();
        obj.setFieldIgnored("cookie");
        obj.setFieldLong(42);
        obj.setFieldString("obj1");

        realm.beginTransaction();
        AllJavaTypes managedAllJavaTypes = realm.copyToRealm(obj);
        realm.commitTransaction();

        realm.beginTransaction();

        AllJavaTypes filedObject = new AllJavaTypes();
        filedObject.setFieldLong(8);
        filedObject = realm.copyToRealm(filedObject);
        managedAllJavaTypes.setFieldObject(filedObject);
        managedAllJavaTypes.setFieldString("updated");

        realm.insertOrUpdate(managedAllJavaTypes);
        realm.commitTransaction();

        AllJavaTypes first = realm.where(AllJavaTypes.class).equalTo(AllJavaTypes.FIELD_LONG, 42).findFirst();
        assertNotNull(first);
        assertEquals(42, first.getFieldLong(), 0);
        assertEquals("updated", first.getFieldString());
        assertNull(first.getFieldIgnored());
        assertNotNull(first.getFieldObject());
        assertEquals(8, first.getFieldObject().getFieldLong());

        assertEquals(2, realm.where(AllJavaTypes.class).findAll().size());
    }
}