package jpabook.jpashop;

import jpabook.jpashop.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class MemberRepositoryTest {
    @Autowired
    MemberRepository_ memberRepository;

    @Test
    @Transactional
    @Rollback(value = false )
    public void testMemeber() throws Exception {
        //given
        Member member = new Member();
        //member.setUsername("memberA");

        //when
        Long saveId = memberRepository.save(member);
        Member findMember = memberRepository.find(saveId);

        //then
        assertThat(findMember.getId()).isEqualTo(member.getId());
        //assertThat(findMember.getUsername()).isEqualTo(member.getUsername());

        assertThat(findMember).isEqualTo(member);
        System.out.println("findMember = " + findMember);
        System.out.println("member = " + member);
    }
}