package com.first.gateway.service.profile;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.domain.enums.SynthesisStatus;
import com.first.gateway.infra.async.TaskHandler;
import com.first.gateway.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("PROFILE_SYNTHESIS")
public class ProfileSynthesisTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(ProfileSynthesisTaskHandler.class);

    private final UserProfileRepository profileRepository;
    private final ProfileSynthesisService profileSynthesisService;

    public ProfileSynthesisTaskHandler(UserProfileRepository profileRepository,
                                        ProfileSynthesisService profileSynthesisService) {
        this.profileRepository = profileRepository;
        this.profileSynthesisService = profileSynthesisService;
    }

    @Override
    public void execute(AsyncTask task) {
        UserProfile profile = profileRepository.findById(task.getRefId())
            .orElseThrow(() -> new RuntimeException("画像不存在"));
        profile.setSynthesisStatus(SynthesisStatus.RUNNING);
        profileRepository.save(profile);
        profileSynthesisService.doSynthesis(profile);
    }

    @Override
    public void onSuccess(AsyncTask task) {
        profileRepository.findById(task.getRefId()).ifPresent(profile -> {
            profile.setSynthesisStatus(SynthesisStatus.IDLE);
            profileRepository.save(profile);
        });
    }

    @Override
    public void onFailure(AsyncTask task, Exception e) {
        profileRepository.findById(task.getRefId()).ifPresent(profile -> {
            profile.setSynthesisStatus(task.getRetryCount() >= task.getMaxRetry()
                ? SynthesisStatus.FAILED : SynthesisStatus.PENDING);
            profileRepository.save(profile);
        });
    }

    @Override
    public int batchSize() { return 3; }
}
