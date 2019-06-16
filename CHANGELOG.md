# act-morphia CHANGE LOG

1.7.0

* Improve aggregation API #32
* Aggregation - use aggregation pipeline to replace group #33

1.6.7 20/Apr/2019
* update to act 1.8.20
* update act-aaa to 1.5.5

1.6.6 23/Dec/2018
* @CreatedAt @LastModifiedAt @CreatedBy @LastModifiedBy not support in morphia #30

1.6.5 09/Dec/2018
* update act to 1.8.16
* @CreatedAt and @LastModifiedAt not support in morphia #28

1.6.4 20/Nov/2018
* update act to 1.8.12
* NPE during app hot reload

1.6.3 04/Nov/2018
* Revert changes that makes `JPADao` be `@Stateless` #26

1.6.2 30/Oct/2018
* update act to 1.8.8
* Make MorphiaDao be stateless
* Make MorphiaService initialization process be async #25
* Catch up `AdaptiveRecord` updates in Act #24
* Update `MorphiaUserBase` - catch up `UserBase` changes in act-aaa-plugin #23
* Add sample data provider for ObjectId #22   

1.6.1 20/Jun/2018
* `MorphiaAuditBase` shall extends from `AuditBase` #21
* `MorphiaUserBase` - need a `setPassword(String)` method #20

1.6.0 19/Jun/2018
* update act to 1.8.8-RC10
* Support act-aaa easy integration model #19

1.5.2 10/Jun/2018
* update act to 1.8.8-RC9

1.5.1 19/May/2018
* update act to 1.8.8-RC5

1.5.0
* Register Type converter between ObjectId and String
* update to act-1.8.8-RC4
* add copy,deepCopy,merge and map method to MorphiaBaseModel

1.4.2
* update act to 1.8.5

1.4.1
* update act to 1.8.2
* update fongo to 2.2.0-RC1

1.4.0
* Catch up to act-1.7.x
* Support inject `Morphia` directly

1.3.0
* Catchup to act-1.6.x

1.2.2 (2017-11-08)
* Add `findLatest()` and `findLastModified()` to `MorphiaDaoBase` class #18
* `MorphiaModelBase._version()` implementation fault #17
* java.lang.IllegalArgumentException: no values supplied #16

1.2.1 (2017-09-10)
* Implement Versioning with osgl-bootstrap #15
* Improve maven build process #14
* NPE when calling aggregation method on an injected Dao #13
* It cannot save model with `BigDecimal` type value in KV store property #11
* Add mapper for `org.osgl.util.Keyword` #10
* It generates useless empty collections in datasource #9 

1.2.0
* update to act-1.4.0
* Special characters in password triggers IllegalArgumentException #8 

1.1.0
- Update to act-1.1.0, using the new DB plugin architecture

1.0.3
- Take out version range from pom.xml. See https://issues.apache.org/jira/browse/MNG-3092

1.0.2
- Act controller not return correct @version "v" for save method when MorphiaDao return the value #5 

1.0.1
- MorphiaInjectionListener not effect on User defined Dao #4 

1.0.0
- first formal release

0.7.0
  - update to actframework 0.7

0.6.0
  - update to actframework 0.6

0.5.1
  - purely version number change as 0.5.0 reserved for techempower test

0.5.0
  - Update to actframework 0.4.0

0.4.1
  - Update to actframework 0.3.1
  - Update morphia to 1.3.2

0.4.0
  - Update to actframework 0.3
  - Update morphia to 1.3.1

0.3.0
  - Update to actframework-0.2.x

0.2.1
  - Update to actframework-0.1.3
  - Update morphia to 1.2.1

0.2.0
  - add query to distinct operation on MorphiaDao #1
  - Add _version() function to MorphiaModel #2
  - Update to actframework-0.1.2
  - Update morphia to 1.2.0
  - Provide actframework _SequenceNumberGenerator implementation

0.1.1
  - baseline version
