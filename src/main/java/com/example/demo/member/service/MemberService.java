package com.example.demo.member.service;

import com.example.demo.config.exception.MemberDuplicationException;
import com.example.demo.config.security.Role;
import com.example.demo.diagnosis.domain.Diagnosis;
import com.example.demo.diagnosis.repository.DiagnosisRepository;
import com.example.demo.diagnosis.service.DiagnosisService;
import com.example.demo.hospital.service.HospitalService;
import com.example.demo.member.domain.Member;
import com.example.demo.member.domain.QMember;
import com.example.demo.member.dto.*;
import com.example.demo.member.repository.MemberRepository;
import com.example.demo.reserve.service.ReserveService;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public  class MemberService implements UserDetailsService {


    private final DiagnosisRepository diagnosisRepository;

    private final MemberRepository memberRepository;

    private final DiagnosisService diagnosisService;

    private final ReserveService reserveService;

    private final HospitalService hospitalService;

    @PersistenceContext
    EntityManager em;



    /**
     * FUNCTION :: 아이디 중복검사
     * @param userEmail
     * @return
     */
    @Transactional(readOnly = true)
    public void validateDuplicateMember(String userEmail) {
        if (memberRepository.findByEmail(userEmail).isPresent()) {
            throw new MemberDuplicationException("Duplicated ID");
        }
    }

    /**
     * FUNCTION : 회원 가입
     * @param form
     * @return
     */
    public ResponseEntity SignUp(MemberDto.Request form) {
        //주소 지정
        form.setAddress(form.getCity(), form.getZipcode(), form.getStreet());

        //패스워드 암호화
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        form.SHA256_PassWord(passwordEncoder.encode(form.getPassword()));


        //권한 부여
        if (form.getRole() == Role.GUEST) {
            form.GIVE_Role(Role.GUEST);
        } else if (form.getRole() == Role.VET) {
            form.GIVE_Role(Role.VET);
        }

        //회원정보 저장
        memberRepository.save(form.toEntity());

        return new ResponseEntity(HttpStatus.OK);
    }

    // 회원 조회
    @Transactional
    public Member findMember(Object id) {
        if (id instanceof Long) { // id 조회
            return memberRepository.findById((Long) id)
                    .orElseThrow(() -> new MemberDuplicationException("해당 회원이 없습니다. id=" + id)); // 조회된 사용자가 없는 경우
        } else { //이메일 조회
            return memberRepository.findByEmail((String) id)
                    .orElseThrow(() -> new MemberDuplicationException("해당 회원이 없습니다. id=" + id)); // 조회된 사용자가 없는 경우
        }
    }

    /**
     * DB에 접근하여 사용자의 정보를 가져온다.
     * @param userEmail
     * @return
     * @throws UsernameNotFoundException
     */
//    @Override
//    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
//        Member userEntityWrapper = memberRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new UsernameNotFoundException("권한 가지고 있는 회원 없음")); // 조회된 사용자가 없는 경우
//
//        GrantedAuthority authority = new SimpleGrantedAuthority(userEntityWrapper.getRole().getValue());
//        UserDetails userDetails =  new User(userEntityWrapper.getEmail(),
//                userEntityWrapper.getPassword(), Arrays.asList(authority));
//
//        return userDetails; // 조회된 유저정보 반환
//    }

    @Override
    public MemberDto.Response loadUserByUsername(String userEmail) throws UsernameNotFoundException{
        return  memberRepository.findByEmail(userEmail).map(u ->
                new MemberDto.Response(u, Collections.singleton(new SimpleGrantedAuthority(u.getRole().getValue())))).orElseThrow(()
                -> new UsernameNotFoundException(userEmail));
    }

    /**
     * FUNCTION :: 회원 정보 수정
     * @param id
     * @param requestDto
     * @return
     */
    @Transactional
    public Long update(Long id, MemberDto.updateInfo requestDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. id=" + id));
        member.update(requestDto.getCity(), requestDto.getStreet(), requestDto.getZipcode(), requestDto.getPhone());
        return id;
    }

    /**
     * FUNCTION :: 회원 패스워드 수정
     * @param id
     * @param requestDto
     * @return
     */
    public Long updatePwd(Long id, MemberDto.updatePwd requestDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. id=" + id));
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String encodePwd = passwordEncoder.encode(requestDto.getPassword());
        member.updatePwd(encodePwd);

        return id;
    }

    /**
     * FUNCTION :: 관리자 / 회원 정보 수정
     * @param id
     * @param requestDto
     * @return
     */
    public Long updateMember(Long id, MemberDto.updateInfo requestDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. id=" + id));

        member.updateMember(requestDto.getName(), requestDto.getCity(), requestDto.getStreet(), requestDto.getZipcode(), requestDto.getPhone());

        return id;
    }

    /**
     * FUNCTION :: 회원 정보 조회
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public MemberDto.Response findById(Long id) {
        Member entity = memberRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(("해당 회원/수의사/관리자가 없습니다. id=" + id)));

        return new MemberDto.Response(entity);
    }

    /**
     * FUNCTION :: 회원정보 삭제
     * @param id
     */
    public void delete(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원/수의사/관리자가 없습니다. id=" + id));
        if (member.getHospital() != null) { //수의사인데 병원을 가지고있는경우
            hospitalService.deleteHospital(member.getHospital().getId()); //예약정보 전부삭제
            memberRepository.delete(member);
        } else if (member.getHospital() == null) { // 수의사인데 병원이 없거나, 일반 사용자일경우
            reserveService.delete_member(member);
            List<Diagnosis> diagnosis = diagnosisRepository.findAllDelete(member);
            diagnosisService.delete(diagnosis);
            memberRepository.delete(member);
        }

    }
    /**
     * FUNCTION :: 사용자정보 전체조회
     * @return
     */

    @Transactional(readOnly = true)
    public List<Member> findAllDesc(MemberDto.MemberSearch m){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        // 이메일, role
        QMember member = QMember.member;
        return queryFactory.select(member)
                    .from(member)
                    .where(nameLike(m.getEmail()),roleStatusEq(m.getRole()))
                    .limit(1000)
                    .fetch();
    }

    private BooleanExpression roleStatusEq(Role role) {
        if(role == null){
            return null;
        }
        return QMember.member.role.eq(role);

    }

    private BooleanExpression nameLike(String email){
        if(!StringUtils.hasText(email)){
            return null;
        }
        return QMember.member.email.eq(email);
    }

//    @Transactional(readOnly = true)
//    public List<MemberDto.Response> findAllDesc() {
//        return memberRepository.findAllDesc().stream()
//                .map(MemberDto.Response::new)
//                .collect(Collectors.toList());
//    }
}
