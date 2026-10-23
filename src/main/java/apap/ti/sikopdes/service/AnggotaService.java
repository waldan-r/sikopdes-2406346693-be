package apap.ti.sikopdes.service;

import apap.ti.sikopdes.dto.Views.AnggotaView;
import apap.ti.sikopdes.mapper.ViewMapper;
import apap.ti.sikopdes.repository.AnggotaRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnggotaService {
  private final AnggotaRepository repository;
  private final ViewMapper mapper;

  public AnggotaService(AnggotaRepository repository, ViewMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public List<AnggotaView> list() {
    return repository.findAll().stream()
        .sorted(Comparator.comparing(a -> a.getId()))
        .map(mapper::anggota)
        .toList();
  }
}
