## 5.5 정리

5장에서는 비즈니스 로직을 담은 UserService 클래스를 만들고 트랜잭션을 적용하면서 스프링의 서비스 추상화에 대해 살펴보았다. 여기서 다룬 주요한 내용은 다음과 같다.

- 비즈니스 로직을 담은 코드는 데이터 액세스 로직을 담은 코드와 분리되는 것이 바람직하다. 비즈니스 로직 코드 또한 내부적으로 책임과 역할에 따라 깔끔하게 메서드로 정리돼야 한다.
- 이를 위해서는 **DAO의 기술 변화에 서비스 계층의 코드가 영향을 받지 않도록 인터페이스와 DI를 잘 활용해서 결합도를 낮춰줘야 한다.**
- DAO를 사용하는 비즈니스 로젝이는 단위 작업을 보장해주는 트랜잭션이 필요하다.
- 트랜잭션의 시작과 종료를 지정하는 일을 **트랜잭션 경계설정**이라고 한다. 트랜잭션 경계 설정은 주로 비즈니스 로직 안에서 일어나는 경우가 많다.
- 시작된 트랜잭션 정보를 담은 오브젝트를 파라미터로 DAO에 전달하는 방법은 매우 비효율적이기 때문에 스프링이 제공하는 트랜잭션 동기화 기법을 활용하는 것이 편리하다.
- 자바에서 사용되는 트랜잭션 API의 종류와 방법은 다양하다. 환경과 서버에 따라서 트랜잭션 방법이 변경되면 경계설정 코드도 함께 변경돼야 한다.
- 트랜잭션 방법에 따라 비즈니스 로직을 담은 코드가 함께 변경되면 단일 책임 원칙에 위배되며, DAO가 사용하는 트겆ㅇ 기술에 대한 강한 결합을 만들어낸다.
- 트랜잭션 경계설정 코드가 비즈니스 로직 코드에 영향을 주지 않게 하려면 스프링이 제공하는 트랜잭션 서비스 추상화를 이용하면 된다.
- 서비스 추상화는 로우레벨의 트랜잭션 기술과 API의 변화에 상관없이 일관된 API를 가진 추상화 계층을 도입한다.
- 서비스 추상화는 테스트하기 어려운 JavaMail 같은 기술에도 적용할 수 있다.