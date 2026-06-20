package ua.foxminded.schoolapplication.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "groups")
public class Group {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_id_seq")
	@SequenceGenerator(name = "group_id_seq", sequenceName = "group_id_seq", allocationSize = 1)
	@Column(name = "group_id", nullable = false)
	private Long id;

	@NotBlank
	@Size(min = 3, max = 21)
	@Pattern(regexp = "^[A-Za-z]+-\\d+$")
	@Column(nullable = false, unique = true)
	private String groupName;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	@OneToMany(mappedBy = "group")
	private List<Student> students = List.of();
}
