package com.muhardin.endy.belajar.vault.service;

import com.muhardin.endy.belajar.vault.dao.MemberDao;
import com.muhardin.endy.belajar.vault.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j @Profile("encrypted")
@Service @Transactional
public class EncryptedMemberService implements MemberInputService {

    @Value("${file.upload.folder}")
    private String fileUploadFolder;

    @Autowired private MemberDao memberDao;
    @Autowired private VaultService vaultService;

    private MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

    @Override
    public void save(Member member, MultipartFile fileKtp) {

        try {
            member.setFileKtpMimeType(fileTypeMap.getContentType(fileKtp.getOriginalFilename()));
            member.setNoKtp(vaultService.encrypt(member.getNoKtp()));
            memberDao.save(member);
            String destinationFilename = fileUploadFolder + File.separator + member.getId();
            log.info("Upload file to {}", destinationFilename);
            FileUtils.writeStringToFile(new File(destinationFilename),
                    vaultService.encrypt(fileKtp.getBytes()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public byte[] getFileKtp(Member member) {
        try {
            String filename = fileUploadFolder + File.separator + member.getId();
            return vaultService.decryptFile(
                    FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return new byte[0];
    }

    @Override
    public Iterable<Member> findAllMembers(){
        List<Member> memberList = new ArrayList<>();
        memberDao.findAll()
                .forEach(member -> {
                    member.setNoKtpPlain(vaultService.decrypt(member.getNoKtp()));
                    memberList.add(member);
                });
        return memberList;
    }
}
