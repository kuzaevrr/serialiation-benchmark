// models/user.go
package models

type User struct {
	ID      string   `json:"id" xml:"id"`
	Name    string   `json:"name" xml:"name"`
	Email   string   `json:"email" xml:"email"`
	Age     int32    `json:"age" xml:"age"`
	Active  bool     `json:"active" xml:"active"`
	Roles   []string `json:"roles" xml:"roles>role"`
	Balance float64  `json:"balance" xml:"balance"`
}
