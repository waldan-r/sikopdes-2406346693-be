package apap.ti.sikopdes.config;

import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.AnggotaRepository;
import java.util.Locale;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MemberSeeder implements CommandLineRunner {
  private final AnggotaRepository anggotaRepository;

  public MemberSeeder(AnggotaRepository anggotaRepository) {
    this.anggotaRepository = anggotaRepository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (anggotaRepository.count() > 0) return;
    Faker faker = new Faker(Locale.forLanguageTag("id-ID"));
    for (int i = 1; i <= 15; i++) {
      Anggota member = new Anggota();
      member.setId(String.format("ANG%04d", i));
      member.setNomorAnggota(String.format("KOP-2026-%04d", i));
      member.setNama(faker.name().fullName());
      member.setEmail(faker.internet().emailAddress());
      member.setTelepon(faker.phoneNumber().phoneNumber());
      member.setStatusKeanggotaan(i <= 12 ? Status.AKTIF : Status.TIDAK_AKTIF);
      anggotaRepository.save(member);
    }
  }
}
